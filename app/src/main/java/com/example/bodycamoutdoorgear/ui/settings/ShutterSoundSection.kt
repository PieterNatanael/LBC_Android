package com.example.bodycamoutdoorgear.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamoutdoorgear.sound.ShutterSound

@Composable
fun ShutterSoundSection(
    selectedId: String,
    onSelect: (ShutterSound) -> Unit,
    onPreview: (ShutterSound) -> Unit,
    sounds: List<ShutterSound> = ShutterSound.builtIn
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "SHUTTER SOUND",
            color = Color.White.copy(0.4f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        sounds.forEach { sound ->
            val selected = sound.id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) Color.White.copy(0.25f) else Color.White.copy(0.1f))
                    .border(
                        1.dp,
                        if (selected) Color.White.copy(0.5f) else Color.White.copy(0.2f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelect(sound) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sound.emoji, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    sound.name,
                    modifier = Modifier.weight(1f),
                    color = if (selected) Color.White else Color.White.copy(0.5f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (selected) {
                    Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                }
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Preview",
                    tint = Color.White.copy(0.6f),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onPreview(sound) }
                )
            }
        }
        Text(
            "Plays at your media volume.",
            color = Color.White.copy(0.3f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
