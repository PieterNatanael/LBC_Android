package com.example.bodycamoutdoorgear.ui.disclaimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.BorderColor
import com.example.bodycamoutdoorgear.ui.theme.ButtonFace
import com.example.bodycamoutdoorgear.ui.theme.CompassGold
import com.example.bodycamoutdoorgear.ui.theme.RecordRed
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary

private data class DisclaimerItem(val icon: ImageVector, val body: String)

private val ITEMS = listOf(
    DisclaimerItem(Icons.Default.Warning,
        "We do not guarantee recordings will be saved. Data loss can occur due to bugs, crashes, or device issues."),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = CompassGold,
                modifier = Modifier.size(48.dp)
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Low Battery Camera",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "IMPORTANT NOTICE",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(32.dp))

            ITEMS.forEachIndexed { index, item ->
                DisclaimerRow(item)
                if (index < ITEMS.lastIndex) {
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 14.dp))
                }
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = ButtonFace)
            ) {
                Text(
                    text = "I Understand",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(32.dp))
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
            text = item.body,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}
