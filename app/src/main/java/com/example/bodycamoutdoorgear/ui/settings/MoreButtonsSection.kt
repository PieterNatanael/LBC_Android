package com.example.bodycamoutdoorgear.ui.settings

import com.example.bodycamoutdoorgear.i18n.tr
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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamoutdoorgear.ui.theme.chromeAccentOr
import com.example.bodycamoutdoorgear.ui.theme.sideButtonStyle
import com.example.bodycamoutdoorgear.ui.theme.themedShape

/**
 * Explains that the row of small buttons beside the shutter holds more than
 * the two that show, and how to reach the rest by swiping.
 */
@Composable
fun MoreButtonsSection() {
    val accent = chromeAccentOr(Color(0xFFFFD966))
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(tr("MORE BUTTONS, JUST SWIPE"))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.06f), themedShape(8.dp))
                .border(1.dp, Color.White.copy(0.15f), themedShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Icons.Default.Cameraswitch, Icons.Default.DarkMode, Icons.Default.FlashOn, Icons.Default.Timer)
                    .forEach { SampleButton(it, accent) }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(0.5f), modifier = Modifier.size(20.dp))
            }
            Body(tr("Next to the shutter you'll see two buttons and a small arrow. There are more hiding behind them. Swipe left or right across the buttons, or tap the arrow, and the row turns like a revolver barrel, bringing the next button in and keeping two in view. Keep swiping to go around again."))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Body(tr("Flash: tap to cycle off, on, auto. On the Video tab it works as a torch that stays lit while you record."))
                Body(tr("Timer (Photo tab): tap to choose 3 or 10 seconds, then press the shutter and a countdown runs before the photo. Press the shutter again to cancel."))
                Body(tr("Both go back to off when you leave the tab."))
            }
        }
    }
}

@Composable
private fun SampleButton(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier.size(38.dp).sideButtonStyle(Color(0xFF1E1E1E), accent),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Body(text: String) {
    Text(text, color = Color.White.copy(0.55f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
}
