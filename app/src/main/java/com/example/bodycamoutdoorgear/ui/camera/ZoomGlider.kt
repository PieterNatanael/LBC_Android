package com.example.bodycamoutdoorgear.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

private val ZOOM_STOPS = listOf(1f, 2f, 5f, 8f)
private const val STOP_TOLERANCE = 0.03f      // relative; camera reports values like 1.99
private const val FULL_RANGE_GLIDE_MS = 1000f // 1x to 8x
private const val MIN_GLIDE_MS = 150f

/**
 * Drives the zoom chip: tap cycles 1x, 2x, 5x, 8x and back to 1x, gliding smoothly
 * between stops. From any zoom that is not a stop, a tap goes straight to 1x.
 * Shared by the video and photo tabs.
 */
class ZoomGlider(
    private val scope: CoroutineScope,
    private val setZoom: (Float) -> Unit
) {
    private var job: Job? = null
    private var target: Float? = null   // where a running glide is heading
    private var lastSet: Float? = null  // most recent value the glide applied

    fun onChipTap(current: Float, min: Float, max: Float) {
        // While gliding, decide from where the glide is heading (not the in-between
        // value), so a quick second tap continues to the next stop instead of being
        // mistaken for "some other zoom" and resetting to 1x.
        val reference = target ?: current
        val next = nextStop(reference, min, max)
        glideTo(from = lastSet ?: current, to = next)
    }

    /** Stops any running glide (pinch, leaving the tab, camera rebind). */
    fun cancel() {
        job?.cancel()
        job = null
        target = null
        lastSet = null
    }

    private fun glideTo(from: Float, to: Float) {
        cancel()
        if (abs(to - from) < 0.001f) return
        target = to
        job = scope.launch {
            val durationMs = (abs(ln(to / from)) / ln(8f) * FULL_RANGE_GLIDE_MS)
                .coerceAtLeast(MIN_GLIDE_MS)
            val startNanos = withFrameNanos { it }
            var t = 0f
            while (t < 1f) {
                val now = withFrameNanos { it }
                t = ((now - startNanos) / 1_000_000f / durationMs).coerceIn(0f, 1f)
                val eased = t * t * (3f - 2f * t)
                // Interpolate the ratio exponentially so the zoom looks like it moves
                // at a constant speed instead of racing at the high end.
                val ratio = from * (to / from).pow(eased)
                lastSet = ratio
                setZoom(ratio)
            }
            setZoom(to)
            target = null
            lastSet = null
        }
    }

    private fun nextStop(reference: Float, min: Float, max: Float): Float {
        val reachable = ZOOM_STOPS.filter { it >= min - 0.001f && it <= max + 0.001f }
        val home = 1f.coerceIn(min, max)
        if (reachable.isEmpty()) return home
        val index = reachable.indexOfFirst { abs(reference - it) <= it * STOP_TOLERANCE }
        return if (index >= 0) reachable[(index + 1) % reachable.size] else home
    }
}

@Composable
fun rememberZoomGlider(setZoom: (Float) -> Unit): ZoomGlider {
    val scope = rememberCoroutineScope()
    val latestSetZoom by rememberUpdatedState(setZoom)
    return remember { ZoomGlider(scope) { latestSetZoom(it) } }
}

fun formatZoom(zoom: Float): String =
    if (abs(zoom - zoom.roundToInt()) < 0.05f) "${zoom.roundToInt()}×" else "%.1f×".format(zoom)
