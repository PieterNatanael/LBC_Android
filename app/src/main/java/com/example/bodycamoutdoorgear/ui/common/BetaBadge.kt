package com.example.bodycamoutdoorgear.ui.common

import com.example.bodycamoutdoorgear.i18n.tr
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Small inline tag for a feature that's new and still being refined —
 * currently just "PeekaBoo". Shared so every entry point reads
 * the same way.
 */
@Composable
fun BetaBadge() {
    Text(
        text = tr("BETA"),
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 9.sp,
        letterSpacing = 1.sp,
        modifier = androidx.compose.ui.Modifier
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
