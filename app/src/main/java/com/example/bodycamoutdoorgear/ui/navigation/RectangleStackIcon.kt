package com.example.bodycamoutdoorgear.ui.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Three cards stacked on top of each other, the ones behind peeking out above the
// front one toward 12 o'clock (like the iOS "rectangle.stack" symbol).
val RectangleStackIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "RectangleStack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            roundRect(x = 6f, y = 2.5f, w = 12f, h = 2f, r = 1f)   // back card
            roundRect(x = 4f, y = 6f, w = 16f, h = 2f, r = 1f)     // middle card
            roundRect(x = 2f, y = 9.5f, w = 20f, h = 12f, r = 2.5f) // front card
        }
    }.build()
}

private fun PathBuilder.roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
    moveTo(x + r, y)
    horizontalLineTo(x + w - r)
    arcToRelative(r, r, 0f, false, true, r, r)
    verticalLineTo(y + h - r)
    arcToRelative(r, r, 0f, false, true, -r, r)
    horizontalLineTo(x + r)
    arcToRelative(r, r, 0f, false, true, -r, -r)
    verticalLineTo(y + r)
    arcToRelative(r, r, 0f, false, true, r, -r)
    close()
}
