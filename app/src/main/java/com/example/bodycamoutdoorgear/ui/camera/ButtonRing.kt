package com.example.bodycamoutdoorgear.ui.camera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.bodycamoutdoorgear.ui.theme.sideButtonStyle
import kotlin.math.roundToInt

private val RingButtonSize = 48.dp
private val RingSpacing = 10.dp
private val RingChevronWidth = 18.dp

/**
 * Left-of-shutter "revolver": [count] buttons on a ring, two visible at a
 * time. Swipe left/right (or tap the chevron) to rotate it indefinitely —
 * swiping left slides the next button in from the right, swiping right
 * brings the previous one in from the left.
 *
 * [button] draws the button at ring index `i`; `enabled` is false while that
 * button is (mostly) faded out so a hidden button can't be tapped.
 */
@Composable
fun ButtonRing(
    count: Int,
    accentColor: Color,
    isAlt: Boolean,
    modifier: Modifier = Modifier,
    button: @Composable (index: Int, enabled: Boolean) -> Unit,
) {
    var steps by remember { mutableIntStateOf(0) }
    val pos by animateFloatAsState(
        targetValue = steps.toFloat(),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "ringPos",
    )
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val stride: Dp = RingButtonSize + RingSpacing
    val stridePx = with(density) { stride.toPx() }
    val thresholdPx = with(density) { 24.dp.toPx() }

    fun rotate(by: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        steps += by
    }

    Box(
        modifier = modifier
            .width(stride * 2 + RingChevronWidth - RingSpacing)
            .height(RingButtonSize)
            .clipToBounds()
            // Watched in the Initial pass, before the buttons see the touch, so a
            // swipe that starts ON a button still turns the ring — and once it
            // moves past the touch slop the move is consumed, which cancels the
            // button's tap. Plain taps (little movement) still reach the buttons.
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var total = 0f
                    var swiping = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        total += change.positionChange().x
                        if (!swiping && kotlin.math.abs(total) > slop) swiping = true
                        if (swiping) change.consume()
                    }
                    if (swiping) {
                        if (total < -thresholdPx / 2) rotate(+1) else if (total > thresholdPx / 2) rotate(-1)
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        for (i in 0 until count) {
            // v in [-1, count-1): 0 and 1 are the two visible slots; the
            // rest are faded out off either end, so the ring wraps unseen.
            val m = (((i - pos + 1f) % count) + count) % count
            val v = m - 1f
            val alpha = when {
                v < 0f -> v + 1f
                v <= 1f -> 1f
                else -> ((2f - v)).coerceIn(0f, 1f)
            }
            // A fully faded button is not composed at all. Left in place at alpha 0
            // it still sits over the neighbouring shutter (the parked slot ends up
            // beside it) and swallows taps on the shutter's left side.
            if (alpha > 0.02f) {
                Box(
                    modifier = Modifier
                        .offset { androidx.compose.ui.unit.IntOffset((v * stridePx).roundToInt(), 0) }
                        .size(RingButtonSize)
                        .alpha(alpha),
                ) { button(i, alpha > 0.6f) }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(RingChevronWidth)
                .height(RingButtonSize)
                .clickable { rotate(+1) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "More buttons",
                tint = if (isAlt) accentColor else Color.White.copy(0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Flash button for the ring. [mode]: 0 = off, 1 = on, 2 = auto. */
@Composable
fun FlashRingButton(
    mode: Int,
    allowsAuto: Boolean,
    accentColor: Color,
    isAlt: Boolean,
    enabled: Boolean,
    onModeChange: (Int) -> Unit,
) {
    val lit = mode != 0
    Box(
        modifier = Modifier
            .size(RingButtonSize)
            .sideButtonStyle(if (lit) Color(0xFF3A3320) else com.example.bodycamoutdoorgear.ui.theme.SurfaceDark, accentColor)
            .clickable(enabled = enabled) { onModeChange((mode + 1) % (if (allowsAuto) 3 else 2)) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            when (mode) {
                0 -> Icons.Default.FlashOff
                2 -> Icons.Default.FlashAuto
                else -> Icons.Default.FlashOn
            },
            contentDescription = "Flash",
            tint = if (lit) Color(0xFFFFD966) else if (isAlt) accentColor else Color.White,
            modifier = Modifier.size(if (isAlt) 26.dp else 22.dp),
        )
    }
}

/** Self-timer button for the ring. [seconds]: 0 = off, 3, 10. */
@Composable
fun TimerRingButton(
    seconds: Int,
    accentColor: Color,
    isAlt: Boolean,
    enabled: Boolean,
    onSecondsChange: (Int) -> Unit,
) {
    val lit = seconds != 0
    Box(
        modifier = Modifier
            .size(RingButtonSize)
            .sideButtonStyle(if (lit) Color(0xFF3A3320) else com.example.bodycamoutdoorgear.ui.theme.SurfaceDark, accentColor)
            .clickable(enabled = enabled) {
                onSecondsChange(if (seconds == 0) 3 else if (seconds == 3) 10 else 0)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (lit) {
            Text(
                text = "${seconds}s",
                color = Color(0xFFFFD966),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
            )
        } else {
            Icon(
                Icons.Default.Timer,
                contentDescription = "Timer",
                tint = if (isAlt) accentColor else Color.White,
                modifier = Modifier.size(if (isAlt) 26.dp else 22.dp),
            )
        }
    }
}
