package com.example.bodycamoutdoorgear.ui.screenrelay

import com.example.bodycamoutdoorgear.i18n.tr
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bodycamoutdoorgear.screenrelay.ScreenRelayPairing
import com.example.bodycamoutdoorgear.screenrelay.ScreenRelayService
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary
import com.example.bodycamoutdoorgear.ui.transfer.QrScannerScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed class Phase {
    object Idle : Phase()
    object Requesting : Phase()
    object Waiting : Phase()
    data class Done(val text: String) : Phase()
    data class Failed(val message: String) : Phase()
    object QuotaExceeded : Phase()
}

/**
 * "PeekaBoo": either the one-time pairing (scan the Mac's QR code),
 * or — once paired — a single button that asks the paired Mac to capture
 * and explain its current screen. The result is shown here and saved into
 * the gallery as a .txt note automatically.
 */
@Composable
fun AnalyzeMyMacScreen(
    onDismiss: () -> Unit,
    tier: String,
    subscriptionManager: com.example.bodycamoutdoorgear.manager.SubscriptionManager,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPaired by remember { mutableStateOf(ScreenRelayPairing.isPaired(context)) }
    var showScanner by remember { mutableStateOf(false) }
    var pairingError by remember { mutableStateOf<String?>(null) }
    // Shown once, the first time anyone opens this screen before pairing —
    // the Mac side needs a real app download, a permission click, and a QR
    // scan, in that order, so a first-time user needs the path spelled out
    // rather than landing straight on "Scan QR Code".
    var introSeen by remember { mutableStateOf(ScreenRelayPairing.introSeen(context)) }
    var showIntroAgain by remember { mutableStateOf(false) }
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showScanner = true
    }

    var prompt by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf<Phase>(Phase.Idle) }
    var showUpgrade by remember { mutableStateOf(false) }

    fun handleScanResult(raw: String) {
        showScanner = false
        val uri = android.net.Uri.parse(raw)
        if (uri.scheme != "lbc" || uri.host != "screen-pair") {
            pairingError = tr("That QR code doesn't look right. Make sure you're scanning the one from LBC Gallery on your Mac.")
            return
        }
        val pairId = uri.getQueryParameter("pair_id")
        if (pairId == null) {
            pairingError = tr("That QR code doesn't look right. Make sure you're scanning the one from LBC Gallery on your Mac.")
            return
        }
        scope.launch {
            try {
                ScreenRelayService.confirmPairing(pairId, ScreenRelayPairing.phoneInstallId(context), tier)
                ScreenRelayPairing.savePairing(context, pairId)
                isPaired = true
            } catch (_: ScreenRelayService.SubscriptionRequiredException) {
                showUpgrade = true
            } catch (_: Exception) {
                pairingError = tr("Couldn't complete pairing. Make sure the code on your Mac is still showing, then try again.")
            }
        }
    }

    suspend fun runAnalysis() {
        val pairId = ScreenRelayPairing.pairId(context)
        if (pairId == null) {
            phase = Phase.Failed(tr("Not linked to a Mac. Please pair again."))
            return
        }
        val phoneId = ScreenRelayPairing.phoneInstallId(context)
        phase = Phase.Requesting

        val jobId = try {
            ScreenRelayService.requestAnalysis(pairId, phoneId, prompt, tier)
        } catch (_: ScreenRelayService.QuotaExceededException) {
            phase = Phase.QuotaExceeded
            return
        } catch (_: Exception) {
            phase = Phase.Failed(tr("Couldn't reach your Mac. Make sure LBC Gallery is open there and it's awake and unlocked."))
            return
        }

        phase = Phase.Waiting
        // Polls for up to ~45s — long enough for the Mac's next poll tick
        // (every 4s) plus the Claude call, short enough that this never
        // feels like it's hanging forever if the Mac is actually asleep.
        repeat(30) {
            delay(1500)
            val status = try { ScreenRelayService.pollResult(jobId, phoneId) } catch (_: Exception) { null }
            when (status) {
                is ScreenRelayService.ResultStatus.Done -> {
                    saveToGallery(context, status.text)
                    phase = Phase.Done(status.text)
                    return
                }
                ScreenRelayService.ResultStatus.Expired -> {
                    phase = Phase.Failed(tr("No answer came back in time. Make sure LBC Gallery is open and awake on your Mac."))
                    return
                }
                else -> Unit // pending or a transient error — keep polling
            }
        }
        phase = Phase.Failed(tr("No answer came back in time. Make sure LBC Gallery is open and awake on your Mac."))
    }

    if (showScanner) {
        QrScannerScreen(onResult = ::handleScanResult, onDismiss = { showScanner = false })
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tr("PeekaBoo"),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                com.example.bodycamoutdoorgear.ui.common.BetaBadge()
            }

            Text(
                tr("Why we built this"),
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.clickable {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://gogoodmood.com/blog/en/peekaboo-analyze-my-mac/"))
                    )
                }
            )

            if (isPaired) {
                PairedContent(
                    prompt = prompt,
                    onPromptChange = { prompt = it },
                    phase = phase,
                    onAnalyze = { scope.launch { runAnalysis() } },
                    onAskAgain = { phase = Phase.Idle; prompt = "" },
                    onTryAgain = { phase = Phase.Idle },
                    onUpgrade = { showUpgrade = true },
                    onUnlink = {
                        ScreenRelayPairing.reset(context)
                        isPaired = false
                        phase = Phase.Idle
                    },
                )
            } else if (!introSeen || showIntroAgain) {
                IntroSteps(
                    onOpenMacAppStore = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://apps.apple.com/app/lbc-gallery/id6812152513"))
                        context.startActivity(intent)
                    },
                    onContinue = {
                        ScreenRelayPairing.setIntroSeen(context)
                        introSeen = true
                        showIntroAgain = false
                    },
                )
            } else if (tier == "free") {
                SubscriptionRequiredContent(onSeePlans = { showUpgrade = true })
            } else {
                PairingContent(
                    error = pairingError,
                    onScan = {
                        pairingError = null
                        if (hasCameraPermission) showScanner = true
                        else permLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onShowIntroAgain = { showIntroAgain = true },
                )
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = tr("Close"), tint = Color.White)
        }
    }

    if (showUpgrade) {
        AnalyzeMyMacUpgradeScreen(
            subscriptionManager = subscriptionManager,
            onDismiss = { showUpgrade = false },
        )
    }
}

@Composable
private fun SubscriptionRequiredContent(onSeePlans: () -> Unit) {
    Text(
        tr("PeekaBoo is for subscribers. A paired Mac polls this server constantly, which only makes sense to offer with a subscription behind it."),
        color = TextSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
    )
    Button(
        onClick = onSeePlans,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(tr("See plans"), fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
private fun PairingContent(error: String?, onScan: () -> Unit, onShowIntroAgain: () -> Unit) {
    Text(
        tr("On your Mac, open LBC Gallery, go to Settings and choose \"Pair with Phone\", then scan the QR code it shows."),
        color = TextSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
    )
    if (error != null) {
        Text(error, color = Color(0xFFE05B5B), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
    Button(
        onClick = onScan,
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(tr("Scan QR Code"), fontWeight = FontWeight.Bold, color = Color.White)
    }
    TextButton(onClick = onShowIntroAgain) {
        Text(tr("How does this work?"), color = Color.White.copy(0.4f), fontSize = 13.sp)
    }
}

@Composable
private fun IntroSteps(onOpenMacAppStore: () -> Unit, onContinue: () -> Unit) {
    IntroStep(1, tr("Download LBC Gallery on your Mac"), tr("It's free on the Mac App Store."))
    IntroStep(2, tr("Open it and start pairing"), tr("Tap the gear icon, then \"Pair with Phone\" — it shows a QR code."))
    IntroStep(3, tr("Allow Screen Recording when asked"), tr("Your Mac will ask permission the first time. Click Allow. If it doesn't work right away, quit and reopen LBC Gallery once — that's a one-time step."))
    IntroStep(4, tr("Come back here and scan"), tr("Tap \"Scan QR Code\" below and point your camera at the code on your Mac."))

    TextButton(onClick = onOpenMacAppStore) {
        Text(tr("Open Mac App Store page"), color = Color.White.copy(0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }

    Button(
        onClick = onContinue,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(tr("Got it, let's scan"), fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
private fun IntroStep(number: Int, title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(26.dp).background(Color.White, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Column {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun PairedContent(
    prompt: String,
    onPromptChange: (String) -> Unit,
    phase: Phase,
    onAnalyze: () -> Unit,
    onAskAgain: () -> Unit,
    onTryAgain: () -> Unit,
    onUpgrade: () -> Unit,
    onUnlink: () -> Unit,
) {
    when (phase) {
        is Phase.Idle -> {
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                label = { Text(tr("Optional: ask something specific")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAnalyze() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(0.5f),
                    unfocusedBorderColor = Color.White.copy(0.2f),
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onAnalyze,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.Black)
                Text(tr("  Use PeekaBoo"), fontWeight = FontWeight.Bold, color = Color.Black)
            }
            TextButton(onClick = onUpgrade) {
                Text(tr("See plans"), color = Color.White.copy(0.4f), fontSize = 13.sp)
            }
            TextButton(onClick = onUnlink) {
                Text(tr("Unlink this Mac"), color = Color.White.copy(0.4f), fontSize = 13.sp)
            }
        }
        is Phase.Requesting, is Phase.Waiting -> {
            CircularProgressIndicator(color = Color.White)
            Text(
                if (phase is Phase.Requesting) tr("Asking your Mac…") else tr("Your Mac is looking at the screen…"),
                color = TextSecondary,
                fontSize = 14.sp,
            )
        }
        is Phase.Done -> {
            Text(
                phase.text,
                color = TextPrimary,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
            )
            Text(tr("Saved to your gallery"), color = Color.White.copy(0.4f), fontSize = 12.sp)
            Button(
                onClick = onAskAgain,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tr("Ask again"), fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        is Phase.Failed -> {
            Text(phase.message, color = Color(0xFFE05B5B), fontSize = 14.sp, textAlign = TextAlign.Center)
            Button(
                onClick = onTryAgain,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tr("Try again"), fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        is Phase.QuotaExceeded -> {
            Text(
                tr("You've used all your PeekaBoo requests for this period."),
                color = TextPrimary, fontSize = 14.sp, textAlign = TextAlign.Center
            )
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tr("See upgrade options"), fontWeight = FontWeight.Bold, color = Color.Black)
            }
            TextButton(onClick = onTryAgain) {
                Text(tr("Not now"), color = Color.White.copy(0.6f))
            }
        }
    }
}

private fun saveToGallery(context: android.content.Context, text: String) {
    val name = "analyze-mac_${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())}.txt"
    File(context.filesDir, name).writeText(text)
}
