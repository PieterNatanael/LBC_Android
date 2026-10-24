package com.example.bodycamoutdoorgear.ui.player

import com.example.bodycamoutdoorgear.i18n.tr
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.runtime.produceState
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodycamoutdoorgear.data.VideoItem
import com.example.bodycamoutdoorgear.manager.SubscriptionManager
import com.example.bodycamoutdoorgear.privategallery.PrivateGalleryStore
import com.example.bodycamoutdoorgear.ui.paywall.PaywallScreen
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.ButtonFace
import com.example.bodycamoutdoorgear.ui.theme.RecordRed
import com.example.bodycamoutdoorgear.ui.theme.SuccessGreen
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary
import com.example.bodycamoutdoorgear.viewmodel.AppearanceMode
import com.example.bodycamoutdoorgear.viewmodel.PlayerViewModel
import com.example.bodycamoutdoorgear.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PhotoViewerScreen(
    photo: VideoItem,
    bitmap: Bitmap?,
    subscriptionManager: SubscriptionManager,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    // Fires after "Set as Private"/"Show in Gallery" actually moves the
    // file — the caller drops it from whatever list is currently on
    // screen, without this view touching that list itself.
    onMoved: () -> Unit = {},
    playerViewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val isPrivate = remember(photo.file) {
        photo.file.parentFile?.path == PrivateGalleryStore.folder(context).path
    }
    var showMoveError by remember { mutableStateOf(false) }
    val isUnlocked = subscriptionManager.isUnlocked
    // `bitmap` is the gallery's small grid thumbnail (about 1000px wide) —
    // fine as a placeholder but visibly soft when stretched to a full screen,
    // and useless for pinch-zoom. Load the real photo and swap it in.
    val fullBitmap by produceState<Bitmap?>(initialValue = null, photo.file) {
        value = withContext(Dispatchers.IO) { decodeViewerBitmap(photo.file) }
    }
    val shownBitmap = fullBitmap ?: bitmap
    val isSaving by playerViewModel.isSaving.collectAsState()
    val saveSuccess by playerViewModel.saveSuccess.collectAsState()
    val saveError by playerViewModel.saveError.collectAsState()

    val settingsViewModel: SettingsViewModel = viewModel()
    val appearanceMode by settingsViewModel.appearanceMode.collectAsState()
    val accentColor = when (appearanceMode) {
        AppearanceMode.SIMPLE    -> androidx.compose.ui.graphics.Color(0xFFCC0000)
        AppearanceMode.TACTICAL  -> androidx.compose.ui.graphics.Color(0xFF00CC44)
        AppearanceMode.MATCHA    -> androidx.compose.ui.graphics.Color(0xFF7BAF52)
        AppearanceMode.ICE_CREAM -> androidx.compose.ui.graphics.Color(0xFFFF8FAB)
        AppearanceMode.TROPICAL  -> androidx.compose.ui.graphics.Color(0xFFA0522D)
        AppearanceMode.SPIDER    -> androidx.compose.ui.graphics.Color(0xFFBB0000)
        AppearanceMode.MARS      -> com.example.bodycamoutdoorgear.ui.theme.MarsRust
        else                     -> BackgroundDark
    }

    var showPaywall by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SimpleDateFormat("MMM d, yyyy  h:mm a", com.example.bodycamoutdoorgear.i18n.Tr.locale).format(photo.createdAt),
                color = TextSecondary,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = tr("Close"), tint = TextSecondary)
            }
        }

        // Photo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            if (shownBitmap != null) {
                var scale by remember(photo.id) { mutableStateOf(1f) }
                var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }
                var lastTapTime by remember(photo.id) { mutableStateOf(0L) }

                Image(
                    bitmap = shownBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(photo.id) {
                            val bmpW = shownBitmap.width.toFloat()
                            val bmpH = shownBitmap.height.toFloat()
                            // How far the zoomed photo can be dragged before its own
                            // edge reaches the edge of the viewer. Based on the photo as
                            // drawn (fitted, then scaled), so a photo that still fits
                            // across the screen has no sideways room and swipes go
                            // straight to the pager.
                            fun panLimits(s: Float): Offset {
                                val fit = minOf(size.width / bmpW, size.height / bmpH)
                                return Offset(
                                    (bmpW * fit * s - size.width).coerceAtLeast(0f) / 2f,
                                    (bmpH * fit * s - size.height).coerceAtLeast(0f) / 2f
                                )
                            }
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var wasMultiTouch = false
                                var dragged = false
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.size >= 2) {
                                        wasMultiTouch = true
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                        val lim = panLimits(newScale)
                                        offset = Offset(
                                            (offset.x + panChange.x * scale).coerceIn(-lim.x, lim.x),
                                            (offset.y + panChange.y * scale).coerceIn(-lim.y, lim.y)
                                        )
                                        scale = newScale
                                        event.changes.forEach { it.consume() }
                                    } else if (!wasMultiTouch && scale > 1f) {
                                        // One finger on a zoomed photo pans it, like the
                                        // native gallery. Only what the photo can absorb is
                                        // consumed: once it is against an edge the drag is
                                        // left alone and the pager swipes to the next photo.
                                        val change = event.changes.first()
                                        val drag = change.positionChange()
                                        if (drag != Offset.Zero) {
                                            val lim = panLimits(scale)
                                            val newX = (offset.x + drag.x).coerceIn(-lim.x, lim.x)
                                            val newY = (offset.y + drag.y).coerceIn(-lim.y, lim.y)
                                            val movedX = newX != offset.x
                                            val movedY = newY != offset.y
                                            offset = Offset(newX, newY)
                                            if (movedX || (movedY && kotlin.math.abs(drag.y) >= kotlin.math.abs(drag.x))) {
                                                change.consume()
                                                dragged = true
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                // Double-tap to toggle zoom, without consuming single-finger
                                // taps/drags so swiping to the next photo keeps working.
                                if (!wasMultiTouch && !dragged) {
                                    val now = System.currentTimeMillis()
                                    if (now - lastTapTime < 300) {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                        }
                                        lastTapTime = 0L
                                    } else {
                                        lastTapTime = now
                                    }
                                }
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            } else {
                CircularProgressIndicator(color = TextSecondary)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Save status
        if (isSaving) {
            Text(
                text = tr("Saving..."),
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else if (saveSuccess) {
            Text(
                text = tr("Saved to Gallery"),
                color = SuccessGreen,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PhotoActionButton(
                icon = Icons.Default.SaveAlt,
                locked = !isUnlocked,
                bgColor = accentColor,
                onClick = {
                    if (!isUnlocked) showPaywall = true
                    else playerViewModel.savePhotoToGallery(photo.file.absolutePath)
                }
            )
            PhotoActionButton(
                icon = Icons.Default.IosShare,
                locked = !isUnlocked,
                bgColor = accentColor,
                onClick = {
                    if (!isUnlocked) {
                        showPaywall = true
                    } else {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            photo.file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, tr("Share Photo")))
                    }
                }
            )
            PhotoActionButton(
                icon = if (isPrivate) Icons.Default.LockOpen else Icons.Default.Lock,
                locked = false,
                bgColor = accentColor,
                onClick = {
                    try {
                        if (isPrivate) PrivateGalleryStore.moveOut(context, photo.file)
                        else PrivateGalleryStore.moveIn(context, photo.file)
                        onMoved()
                    } catch (e: Exception) {
                        showMoveError = true
                    }
                }
            )
            PhotoActionButton(
                icon = Icons.Default.Delete,
                locked = false,
                bgColor = accentColor,
                tint = RecordRed,
                onClick = { showDeleteConfirm = true }
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(tr("Delete Photo?"), color = TextPrimary) },
            text = { Text(tr("This photo will be permanently deleted."), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDeleted() }) {
                    Text(tr("Delete"), color = RecordRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(tr("Cancel"), color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    if (showMoveError) {
        AlertDialog(
            onDismissRequest = { showMoveError = false },
            title = { Text(tr("Couldn't move this photo"), color = TextPrimary) },
            text = { Text(tr("Please try again."), color = TextSecondary) },
            confirmButton = {
                Button(onClick = { showMoveError = false }) { Text(tr("OK")) }
            },
            containerColor = SurfaceDark
        )
    }

    if (saveError != null) {
        AlertDialog(
            onDismissRequest = playerViewModel::clearSaveError,
            title = { Text(tr("Save Failed"), color = TextPrimary) },
            text = { Text(saveError ?: "", color = TextSecondary) },
            confirmButton = {
                Button(onClick = playerViewModel::clearSaveError) { Text(tr("OK")) }
            },
            containerColor = SurfaceDark
        )
    }

    if (showPaywall) {
        PaywallScreen(
            subscriptionManager = subscriptionManager,
            onDismiss = { showPaywall = false }
        )
    }
}

@Composable
private fun PhotoActionButton(
    icon: ImageVector,
    locked: Boolean,
    bgColor: androidx.compose.ui.graphics.Color = BackgroundDark,
    tint: androidx.compose.ui.graphics.Color = TextPrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(bgColor, androidx.compose.foundation.shape.CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Longest side kept for the viewer. Big enough to zoom into, small enough
 *  that a few of these in memory at once (the pager keeps neighbours) stay safe. */
private const val VIEWER_MAX_DIMENSION = 3072

private fun decodeViewerBitmap(file: java.io.File): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= VIEWER_MAX_DIMENSION) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = BitmapFactory.decodeFile(file.absolutePath, opts)
    val rotation = when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90  -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (decoded != null && rotation != 0f) {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
            .also { decoded.recycle() }
    } else decoded
} catch (e: Exception) {
    null
}
