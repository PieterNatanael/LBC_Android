package com.example.bodycamoutdoorgear.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamoutdoorgear.ui.theme.chromeAccentOr
import com.example.bodycamoutdoorgear.ui.theme.sideButtonStyle
import com.example.bodycamoutdoorgear.ui.theme.themedShape

/**
 * Explains the moon button next to the shutter ("Turn Off the Light!") and
 * carries the privacy reminder that goes with recording covertly.
 */
@Composable
fun TurnOffLightSection() {
    val accent = chromeAccentOr(Color(0xFFFFD966))
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel("TURN OFF THE LIGHT!")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.06f), themedShape(8.dp))
                .border(1.dp, Color.White.copy(0.15f), themedShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).sideButtonStyle(Color(0xFF1E1E1E), accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Text(
                    "The moon button next to the shutter",
                    color = Color.White.copy(0.8f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Turn Off the Light! lets you keep recording without keeping your whole screen awake. Tap the moon and the screen goes dark while the camera keeps working; tap the screen to wake it.",
                color = Color.White.copy(0.55f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PanTool, contentDescription = null, tint = Color.White.copy(0.6f), modifier = Modifier.size(14.dp).padding(top = 2.dp))
                Text(
                    "Please be mindful of people's privacy. Ask for permission before recording someone.",
                    color = Color.White.copy(0.75f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
