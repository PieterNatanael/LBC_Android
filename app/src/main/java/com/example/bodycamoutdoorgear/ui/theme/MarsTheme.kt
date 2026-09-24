package com.example.bodycamoutdoorgear.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Two opposite corners cut at an angle, like a cockpit panel. Mars uses this
// where other themes use rounded corners; the cut is capped so tall panels
// don't get oversized bevels.
val MarsChamferShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cut = minOf(minOf(size.width, size.height) * 0.24f, 12f * density.density)
        val path = Path().apply {
            moveTo(cut, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - cut)
            lineTo(size.width - cut, size.height)
            lineTo(0f, size.height)
            lineTo(0f, cut)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun MarsRingTicks(ringSize: Dp, color: Color) {
    listOf(0f, 90f, 270f).forEach { angle ->
        Box(
            Modifier
                .rotate(angle)
                .offset(y = -(ringSize / 2 + 8.dp))
                .size(width = 3.dp, height = 8.dp)
                .background(color)
        )
    }
    Text(
        "M-28",
        color = color,
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.offset(y = ringSize / 2 + 8.dp)
    )
}

@Composable
private fun MarsXMark(size: Dp) {
    Canvas(Modifier.size(size)) {
        val inset = this.size.width * 0.28f
        val stroke = 5.dp.toPx()
        drawLine(Color.Black, Offset(inset, inset), Offset(this.size.width - inset, this.size.height - inset), stroke, StrokeCap.Round)
        drawLine(Color.Black, Offset(this.size.width - inset, inset), Offset(inset, this.size.height - inset), stroke, StrokeCap.Round)
    }
}

@Composable
fun MarsShutterButton(isCapturing: Boolean, onClick: () -> Unit, size: Dp = 70.dp) {
    // Same footprint as the other themes' buttons; the ticks and label
    // overflow it rather than growing it, which would push the whole
    // control row up.
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        MarsRingTicks(ringSize = size, color = MarsRust)
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(3.dp, Color.White, CircleShape)
                .background(Color.Black)
                .clickable(enabled = !isCapturing, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(size * 0.78f)
                    .clip(CircleShape)
                    .background(if (isCapturing) MarsRust.copy(0.4f) else MarsRust),
                contentAlignment = Alignment.Center
            ) { MarsXMark(size * 0.78f) }
        }
    }
}

@Composable
fun MarsRecordButton(isRecording: Boolean, onClick: () -> Unit, size: Dp = 70.dp) {
    // Same footprint as the other themes' buttons; the ticks and label
    // overflow it rather than growing it, which would push the whole
    // control row up.
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        MarsRingTicks(ringSize = size, color = MarsRust)
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(3.dp, if (isRecording) MarsRust else Color.White, CircleShape)
                .background(Color.Black)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (isRecording) {
                Box(Modifier.size(size * 0.32f).background(Color.White))
            } else {
                Box(
                    Modifier.size(size * 0.78f).clip(CircleShape).background(MarsRust),
                    contentAlignment = Alignment.Center
                ) { MarsXMark(size * 0.78f) }
            }
        }
    }
}

val LocalMarsTheme = compositionLocalOf { false }

/** Rounded corners normally, Mars's angled cockpit-panel cut when Mars is active. */
@Composable
fun themedShape(radius: Dp): Shape =
    if (LocalMarsTheme.current) MarsChamferShape else RoundedCornerShape(radius)

/** The active theme's accent for Settings selected states and labels, or null for Normal. */
val LocalChromeAccent = compositionLocalOf<Color?> { null }

@Composable
@ReadOnlyComposable
fun chromeAccentOr(default: Color): Color = LocalChromeAccent.current ?: default

/**
 * The round dark buttons beside the shutter/record button. On Mars they
 * become black angled panels outlined in the theme accent, matching the iOS
 * build; every other theme keeps the plain round fill.
 */
@Composable
fun Modifier.sideButtonStyle(fill: Color, accent: Color): Modifier =
    if (LocalMarsTheme.current) {
        this.clip(MarsChamferShape).background(Color.Black).border(2.dp, accent, MarsChamferShape)
    } else {
        this.background(fill, CircleShape)
    }
