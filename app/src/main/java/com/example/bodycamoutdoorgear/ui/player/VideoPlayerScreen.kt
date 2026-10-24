package com.example.bodycamoutdoorgear.ui.player

import com.example.bodycamoutdoorgear.i18n.tr
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.bodycamoutdoorgear.data.VideoItem
import com.example.bodycamoutdoorgear.manager.SubscriptionManager
import com.example.bodycamoutdoorgear.privategallery.PrivateGalleryStore
import com.example.bodycamoutdoorgear.ui.paywall.PaywallScreen
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.BorderColor
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
fun VideoPlayerScreen(
    video: VideoItem,
    exoPlayer: ExoPlayer,
    subscriptionManager: SubscriptionManager,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    // Fires after "Set as Private"/"Show in Gallery" actually moves the
    // file — see PhotoViewerScreen's identical parameter.
    onMoved: () -> Unit = {},
    isActive: Boolean = true,
    playerViewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val isPrivate = remember(video.file) {
        video.file.parentFile?.path == PrivateGalleryStore.folder(context).path
    }
    var showMoveError by remember { mutableStateOf(false) }
    val isUnlocked = subscriptionManager.isUnlocked
    val isSaving by playerViewModel.isSaving.collectAsState()
    val saveSuccess by playerViewModel.saveSuccess.collectAsState()
    val saveError by playerViewModel.saveError.collectAsState()

    val settingsViewModel: SettingsViewModel = viewModel()
    val appearanceMode by settingsViewModel.appearanceMode.collectAsState()
    val accentColor = when (appearanceMode) {
        AppearanceMode.SIMPLE    -> Color(0xFFCC0000)
        AppearanceMode.TACTICAL  -> Color(0xFF00CC44)
        AppearanceMode.MATCHA    -> Color(0xFF7BAF52)
        AppearanceMode.ICE_CREAM -> Color(0xFFFF8FAB)
        AppearanceMode.TROPICAL  -> Color(0xFFA0522D)
        AppearanceMode.SPIDER    -> Color(0xFFBB0000)
        AppearanceMode.MARS      -> com.example.bodycamoutdoorgear.ui.theme.MarsRust
        else                     -> BackgroundDark
    }

    var showPaywall by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE) }
    var loopEnabled by remember { mutableStateOf(prefs.getBoolean("loop_enabled", true)) }

    // exoPlayer is shared across every page of the swipe viewer (see GalleryScreen) —
    // only the active page may point it at its own file, so an inactive/background
    // page (briefly composed during a swipe transition) never steals playback.
    LaunchedEffect(video.id, isActive) {
        if (isActive) {
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(video.file)))
            exoPlayer.repeatMode = if (loopEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isActive) {
        val observer = LifecycleEventObserver { _, event ->
            if (isActive && event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isActive) {
        if (isActive) exoPlayer.play() else exoPlayer.pause()
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = SimpleDateFormat("MMM d, yyyy  h:mm a", com.example.bodycamoutdoorgear.i18n.Tr.locale).format(video.createdAt),
                color = TextSecondary,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    loopEnabled = !loopEnabled
                    prefs.edit().putBoolean("loop_enabled", loopEnabled).apply()
                    exoPlayer.repeatMode = if (loopEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                }) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = tr("Loop"),
                        tint = if (loopEnabled) TextPrimary else TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = tr("Close"), tint = TextSecondary)
                }
            }
        }

        // Video player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            var scale by remember(video.id) { mutableStateOf(1f) }
            var offset by remember(video.id) { mutableStateOf(Offset.Zero) }
            var lastTapTime by remember(video.id) { mutableStateOf(0L) }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        controllerAutoShow = false
                    }
                },
                update = { view -> view.player = if (isActive) exoPlayer else null },
                onRelease = { view -> view.player = null },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(video.id) {
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

                            // Double-tap to reset zoom, without consuming single taps
                            // (so PlayerView's own controls toggle keeps working).
                            if (!wasMultiTouch && scale > 1f) {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTime < 300) {
                                    scale = 1f
                                    offset = Offset.Zero
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
            ActionButton(
                icon = Icons.Default.SaveAlt,
                locked = !isUnlocked,
                bgColor = accentColor,
                onClick = {
                    if (!isUnlocked) showPaywall = true
                    else playerViewModel.saveToGallery(video.file.absolutePath)
                }
            )
            ActionButton(
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
                            video.file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, tr("Share Recording")))
                    }
                }
            )
            ActionButton(
                icon = if (isPrivate) Icons.Default.LockOpen else Icons.Default.Lock,
                locked = false,
                bgColor = accentColor,
                onClick = {
                    try {
                        if (isPrivate) PrivateGalleryStore.moveOut(context, video.file)
                        else PrivateGalleryStore.moveIn(context, video.file)
                        onMoved()
                    } catch (e: Exception) {
                        showMoveError = true
                    }
                }
            )
            ActionButton(
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
            title = { Text(tr("Delete Recording?"), color = TextPrimary) },
            text = { Text(tr("This recording will be permanently deleted."), color = TextSecondary) },
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
            title = { Text(tr("Couldn't move this video"), color = TextPrimary) },
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
private fun ActionButton(
    icon: ImageVector,
    locked: Boolean,
    bgColor: Color = BackgroundDark,
    tint: Color = TextPrimary,
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
