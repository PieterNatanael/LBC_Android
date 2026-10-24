package com.example.bodycamoutdoorgear.ui.disclaimer

import com.example.bodycamoutdoorgear.i18n.trf
import com.example.bodycamoutdoorgear.i18n.tr
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamoutdoorgear.R
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.BorderColor
import com.example.bodycamoutdoorgear.ui.theme.ButtonFace
import com.example.bodycamoutdoorgear.ui.theme.CompassGold
import com.example.bodycamoutdoorgear.ui.theme.RecordRed
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary

private data class DisclaimerItem(val icon: ImageVector, val body: String)

private data class PermissionItem(val icon: ImageVector, val title: String, val detail: String)

// What the app needs and why — the same structure as the iOS intro, but only
// what Android actually involves: no notifications row, since Android has no
// alarm or reminder feature.
private val PERMISSIONS = listOf(
    PermissionItem(Icons.Default.CameraAlt, "CAMERA",
        "To record video and take photos. The app cannot work without this."),
    PermissionItem(Icons.Default.Mic, "MICROPHONE",
        "To record sound along with your videos."),
    PermissionItem(Icons.Default.PhotoLibrary, "PHOTOS",
        "Only used when you choose to save a photo or video to your gallery. Importing uses the system picker, so LBC sees only what you select."),
    PermissionItem(Icons.Default.Wifi, "WI-FI",
        "Only used to send files between your devices with HipHop.")
)

private val ITEMS = listOf(
    DisclaimerItem(Icons.Default.Warning,
        "We do not guarantee photos and videos will be saved. Data loss can occur due to bugs, crashes, or device issues."),
    DisclaimerItem(Icons.Default.Warning,
        "App updates may cause unexpected behaviour that could affect recording reliability."),
    DisclaimerItem(Icons.Default.Warning,
        "Interrupted recordings (low battery, force-quit, calls) may result in corrupted or missing files."),
    DisclaimerItem(Icons.Default.Warning,
        "Always back up important footage to a safe location immediately after recording."),
    DisclaimerItem(Icons.Default.Warning,
        "This app is provided as-is. We are not liable for any data loss or corruption.")
)

@Composable
fun DisclaimerScreen(onDismiss: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Scrollable content — bottom padding reserves space for the fixed button
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 88.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))

                // The real app icon, so the first thing someone sees matches
                // the icon they just tapped on the home screen. A copy of the
                // iOS icon artwork in drawable-nodpi (a launcher icon is an
                // adaptive icon and can't be drawn directly), so it needs
                // refreshing whenever the app icon changes.
                Image(
                    painter = painterResource(R.drawable.lbc_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0xFF595959), RoundedCornerShape(18.dp))
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = tr("LBC"),
                    color = TextPrimary,
                    fontSize = 30.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = tr("WELCOME"),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = tr("Here is what the app needs, and why."),
                    color = TextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    PERMISSIONS.forEach { PermissionRow(it) }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = tr("Everything you record stays on your device. The one exception is the optional Fill with AI button: only when you tap it, a small copy of that photo (or a few frames from your video) is sent to our server and an AI service to write the card text."),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    text = tr("BEFORE YOU START"),
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )

                Spacer(Modifier.height(16.dp))

                ITEMS.forEachIndexed { index, item ->
                    DisclaimerRow(item)
                    if (index < ITEMS.lastIndex) {
                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 14.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // Fixed button always pinned at the bottom
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = ButtonFace)
            ) {
                Text(
                    text = tr("I Understand"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(item: PermissionItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = CompassGold,
            modifier = Modifier.size(24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = tr(item.title),
                color = TextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = tr(item.detail),
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun DisclaimerRow(item: DisclaimerItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = RecordRed,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Text(
            text = tr(item.body),
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}
