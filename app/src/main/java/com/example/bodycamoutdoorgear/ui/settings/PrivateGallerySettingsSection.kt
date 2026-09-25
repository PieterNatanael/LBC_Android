package com.example.bodycamoutdoorgear.ui.settings

import androidx.compose.foundation.background
import com.example.bodycamoutdoorgear.ui.theme.themedShape
import com.example.bodycamoutdoorgear.ui.theme.chromeAccentOr
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bodycamoutdoorgear.privategallery.PrivateGalleryStore
import com.example.bodycamoutdoorgear.ui.gallery.PrivateGalleryLockMode
import com.example.bodycamoutdoorgear.ui.gallery.PrivateGalleryLockScreen
import com.example.bodycamoutdoorgear.ui.theme.RecordRed
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark

/** Which PIN screen this section's own flow is currently showing. SetUp is
 *  a brand new PIN with nothing to verify first; the other two both start
 *  by confirming the EXISTING PIN before doing anything further. */
private sealed class PrivateGallerySettingsFlow {
    object SetUp : PrivateGallerySettingsFlow()
    object VerifyForChangePin : PrivateGallerySettingsFlow()
    object VerifyForDeactivate : PrivateGallerySettingsFlow()

    val lockMode: PrivateGalleryLockMode get() = when (this) {
        SetUp -> PrivateGalleryLockMode.SETUP
        VerifyForChangePin, VerifyForDeactivate -> PrivateGalleryLockMode.UNLOCK
    }
}

/**
 * Setup and day-to-day unlocking both live in the Gallery tab's own
 * dropdown, reachable without ever opening Settings — this section is only
 * for the two things that specifically belong here: changing an existing
 * PIN, and turning the whole feature off (or resetting it outright).
 *
 * Its own file rather than more inline code in CameraScreen.kt, which
 * already holds the rest of this settings sheet at ~80KB.
 */
@Composable
fun PrivateGallerySettingsSection() {
    val context = LocalContext.current
    // Tracked explicitly rather than re-read from PrivateGalleryStore on
    // every recomposition — confirmed on the iOS build that a plain live
    // read like that can go stale right after a PIN screen dismisses, since
    // dismissal doesn't reliably land in time for the presenter's very next
    // redraw. Set here, once, the moment each action actually completes.
    var isActive by remember { mutableStateOf(PrivateGalleryStore.isActive(context)) }
    var flow by remember { mutableStateOf<PrivateGallerySettingsFlow?>(null) }
    // The second step of "Change PIN" specifically — its own slot rather
    // than reusing `flow`, for the same reason GalleryScreen's own
    // showPrivateGallery is separate from its lock-flow state: chaining two
    // presentations through one Dialog's visibility would race against its
    // own dismiss.
    var showNewPinAfterVerify by remember { mutableStateOf(false) }
    // "Forgot PIN?" lives only here, not on the PIN screen itself — that
    // screen is exactly what someone without the PIN is staring at, and a
    // destructive reset one tap away from every failed guess was too easy
    // to trigger by accident. Reaching Settings and finding this instead is
    // enough extra deliberateness that the reset stays a genuine choice.
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("PRIVATE GALLERY")
        Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            if (isActive) {
                HelperText("Active. Open it from the dropdown under the Gallery tab's title.")
                SettingsRowButton("CHANGE PIN") { flow = PrivateGallerySettingsFlow.VerifyForChangePin }
                SettingsRowButton("TURN OFF PRIVATE GALLERY") { flow = PrivateGallerySettingsFlow.VerifyForDeactivate }
                HelperText("Moves any private photos and videos back into the normal gallery. Nothing is deleted.")
                SettingsRowButton("FORGOT PIN? RESET PRIVATE GALLERY", destructive = true) { showResetConfirm = true }
                HelperText("Permanently deletes every private photo and video. There is no other way to recover a forgotten PIN, by design.")
            } else {
                HelperText("Not set up. \"Set as Private\" still works. Set up a PIN to actually open and view what's moved here.")
                SettingsRowButton("SET UP PRIVATE GALLERY") { flow = PrivateGallerySettingsFlow.SetUp }
            }
        }
    }

    flow?.let { f ->
        FullScreenDialog(onDismissRequest = { flow = null }) {
            PrivateGalleryLockScreen(
                mode = f.lockMode,
                onUnlocked = {
                    when (f) {
                        PrivateGallerySettingsFlow.SetUp -> isActive = true
                        PrivateGallerySettingsFlow.VerifyForChangePin -> showNewPinAfterVerify = true
                        PrivateGallerySettingsFlow.VerifyForDeactivate -> {
                            // Correct PIN just proved ownership — moves
                            // every private file back to the normal gallery
                            // rather than deleting anything, and clears the
                            // PIN.
                            PrivateGalleryStore.deactivateKeepingFiles(context)
                            isActive = false
                        }
                    }
                    flow = null
                },
                onDismiss = { flow = null }
            )
        }
    }

    if (showNewPinAfterVerify) {
        // Plain SETUP: this IS the new PIN being created, straight through
        // to PrivateGalleryStore.setPin — nothing further to chain once it
        // succeeds. Already active going in, so isActive has nothing to
        // change here.
        FullScreenDialog(onDismissRequest = { showNewPinAfterVerify = false }) {
            PrivateGalleryLockScreen(
                mode = PrivateGalleryLockMode.SETUP,
                onUnlocked = { showNewPinAfterVerify = false },
                onDismiss = { showNewPinAfterVerify = false }
            )
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Private Gallery?", color = Color.White) },
            text = {
                Text(
                    "This permanently deletes every private photo and video and turns Private Gallery off. This cannot be undone.",
                    color = Color.White.copy(0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    PrivateGalleryStore.wipe(context)
                    isActive = false
                    showResetConfirm = false
                }) { Text("Delete Everything", color = RecordRed) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel", color = Color.White.copy(0.6f)) }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        color = chromeAccentOr(Color.White.copy(0.4f)),
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 3.sp
    )
}

@Composable
internal fun HelperText(text: String) {
    Text(text, color = Color.White.copy(0.5f), fontSize = 11.sp)
}

/** Change PIN/Turn Off/Set Up all share this rather than each hand-rolling
 *  the same button chrome. */
@Composable
internal fun SettingsRowButton(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = if (destructive) RecordRed else Color.White
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(0.08f), themedShape(6.dp))
            .border(1.dp, if (destructive) RecordRed.copy(0.4f) else Color.White.copy(0.2f), themedShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(label, color = tint, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

/** A Dialog that fills the screen, used to present the PIN screen from Settings. */
@Composable
private fun FullScreenDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
