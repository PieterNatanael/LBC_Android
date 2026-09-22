package com.example.bodycamoutdoorgear.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamoutdoorgear.privategallery.PrivateGalleryStore
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.RecordRed
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary
import kotlinx.coroutines.delay

enum class PrivateGalleryLockMode { SETUP, UNLOCK }

/**
 * The PIN screen for Private Gallery — both creating a PIN for the first
 * time and entering an existing one, since the two only differ in copy and
 * in what happens once 4 digits are in.
 *
 * Biometric unlock (fingerprint/face) is deliberately NOT here yet —
 * androidx.biometric.BiometricPrompt needs a FragmentActivity to host it,
 * and MainActivity is a plain ComponentActivity today. That's a Phase 4
 * change, not something to slip into the PIN screen unannounced.
 *
 * "Forgot PIN?" also isn't here — same call as the iOS build: a destructive
 * reset one tap away from the exact screen someone without the PIN is
 * staring at is too easy to trigger by accident. It lives only in Settings.
 */
@Composable
fun PrivateGalleryLockScreen(
    mode: PrivateGalleryLockMode,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    // Set once the first of setup's two entries lands, so the second entry
    // can be compared against it. Never touched in UNLOCK mode.
    var setupFirstPin by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockedOutUntilMillis by remember { mutableStateOf(0L) }
    var wipedMessageShown by remember { mutableStateOf(false) }
    // Only used to force a recomposition of the countdown text once a
    // second — PrivateGalleryStore itself is what actually enforces it.
    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lockedOutUntilMillis) {
        while (lockedOutUntilMillis > System.currentTimeMillis()) {
            delay(250)
            nowTick = System.currentTimeMillis()
        }
    }

    val isLockedOut = lockedOutUntilMillis > nowTick
    val lockoutSecondsRemaining = ((lockedOutUntilMillis - nowTick) / 1000.0).let {
        if (it <= 0) 0 else kotlin.math.ceil(it).toInt()
    }

    val title = when (mode) {
        PrivateGalleryLockMode.UNLOCK -> "Enter PIN"
        PrivateGalleryLockMode.SETUP  -> if (setupFirstPin == null) "Create a PIN" else "Confirm PIN"
    }

    fun handleSetupEntry() {
        val first = setupFirstPin
        if (first == null) {
            setupFirstPin = enteredPin
            enteredPin = ""
        } else if (enteredPin == first) {
            PrivateGalleryStore.setPin(context, enteredPin)
            onUnlocked()
        } else {
            errorMessage = "PINs didn't match. Start over."
            setupFirstPin = null
            enteredPin = ""
        }
    }

    fun handleUnlockEntry() {
        when (val result = PrivateGalleryStore.verify(context, enteredPin)) {
            is PrivateGalleryStore.VerifyResult.Success -> onUnlocked()
            is PrivateGalleryStore.VerifyResult.WrongPin -> {
                errorMessage = "Wrong PIN"
                enteredPin = ""
            }
            is PrivateGalleryStore.VerifyResult.LockedOut -> {
                lockedOutUntilMillis = result.untilMillis
                enteredPin = ""
            }
            is PrivateGalleryStore.VerifyResult.WipedAfterTooManyAttempts -> {
                enteredPin = ""
                wipedMessageShown = true
            }
        }
    }

    fun onDigit(digit: String) {
        errorMessage = null
        if (enteredPin.length >= 4) return
        enteredPin += digit
        if (enteredPin.length == 4) {
            when (mode) {
                PrivateGalleryLockMode.SETUP  -> handleSetupEntry()
                PrivateGalleryLockMode.UNLOCK -> handleUnlockEntry()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, end = 20.dp), horizontalArrangement = Arrangement.End) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextSecondary,
                    modifier = Modifier.size(26.dp).clickable(onClick = onDismiss)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(20.dp))
                Text(title, color = TextPrimary, fontSize = 18.sp)
                Spacer(Modifier.height(24.dp))

                if (wipedMessageShown) {
                    Text(
                        "Private Gallery has been reset after too many wrong attempts. Everything inside it has been deleted.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    TextButton(onClick = onDismiss) { Text("OK", color = TextPrimary) }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(if (index < enteredPin.length) Color.White else Color.Transparent)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: if (isLockedOut) "Try again in ${lockoutSecondsRemaining}s" else " ",
                        color = if (errorMessage != null) RecordRed.copy(alpha = 0.8f) else TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(24.dp))

                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "⌫")
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        rows.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                row.forEach { key ->
                                    if (key.isEmpty()) {
                                        Spacer(Modifier.size(68.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(CircleShape)
                                                .background(SurfaceDark)
                                                .then(
                                                    if (!isLockedOut) Modifier.clickable {
                                                        if (key == "⌫") {
                                                            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                                        } else {
                                                            onDigit(key)
                                                        }
                                                    } else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (key == "⌫") {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.Backspace,
                                                    contentDescription = "Delete",
                                                    tint = TextPrimary.copy(alpha = if (isLockedOut) 0.3f else 1f)
                                                )
                                            } else {
                                                Text(
                                                    key,
                                                    color = TextPrimary.copy(alpha = if (isLockedOut) 0.3f else 1f),
                                                    fontSize = 26.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
