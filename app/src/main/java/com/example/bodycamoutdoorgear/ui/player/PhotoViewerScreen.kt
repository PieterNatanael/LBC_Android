package com.example.bodycamoutdoorgear.ui.player

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
    playerViewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val isUnlocked = subscriptionManager.isUnlocked
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
                text = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault()).format(photo.createdAt),
                color = TextSecondary,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
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
            if (bitmap != null) {
                var scale by remember(photo.id) { mutableStateOf(1f) }
                var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }
                var lastTapTime by remember(photo.id) { mutableStateOf(0L) }

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(photo.id) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var wasMultiTouch = false
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.size >= 2) {
                                        wasMultiTouch = true
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                        val maxX = (size.width * (newScale - 1f)).coerceAtLeast(0f) / 2f
                                        val maxY = (size.height * (newScale - 1f)).coerceAtLeast(0f) / 2f
                                        offset = Offset(
                                            (offset.x + panChange.x * scale).coerceIn(-maxX, maxX),
                                            (offset.y + panChange.y * scale).coerceIn(-maxY, maxY)
                                        )
                                        scale = newScale
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })

                                // Double-tap to toggle zoom, without consuming single-finger
                                // taps/drags so swiping to the next photo keeps working.
                                if (!wasMultiTouch) {
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
                text = "Saving...",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else if (saveSuccess) {
            Text(
                text = "Saved to Gallery",
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
                        context.startActivity(Intent.createChooser(intent, "Share Photo"))
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
            title = { Text("Delete Photo?", color = TextPrimary) },
            text = { Text("This photo will be permanently deleted.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDeleted() }) {
                    Text("Delete", color = RecordRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    if (saveError != null) {
        AlertDialog(
            onDismissRequest = playerViewModel::clearSaveError,
            title = { Text("Save Failed", color = TextPrimary) },
            text = { Text(saveError ?: "", color = TextSecondary) },
            confirmButton = {
                Button(onClick = playerViewModel::clearSaveError) { Text("OK") }
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
