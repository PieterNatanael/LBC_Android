package com.example.bodycamoutdoorgear.ui.gallery

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodycamoutdoorgear.manager.SubscriptionManager
import com.example.bodycamoutdoorgear.viewmodel.AppearanceMode
import com.example.bodycamoutdoorgear.viewmodel.SettingsViewModel
import com.example.bodycamoutdoorgear.ui.player.PhotoViewerScreen
import com.example.bodycamoutdoorgear.ui.player.VideoPlayerScreen
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.BorderColor
import com.example.bodycamoutdoorgear.ui.theme.RecordRed
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary
import com.example.bodycamoutdoorgear.viewmodel.GalleryViewModel
import com.example.bodycamoutdoorgear.ui.transfer.ReceiveScreen
import com.example.bodycamoutdoorgear.ui.transfer.SendScreen
import androidx.compose.material.icons.filled.Wifi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    subscriptionManager: SubscriptionManager,
    onSelectModeChanged: (Boolean) -> Unit = {},
    galleryViewModel: GalleryViewModel = viewModel()
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel()
    val appearanceMode by settingsViewModel.appearanceMode.collectAsState()
    val showTimestamp  by settingsViewModel.showGalleryTimestamp.collectAsState()
    val galleryColumns by settingsViewModel.galleryColumns.collectAsState()
    val isAlt = appearanceMode != AppearanceMode.NORMAL

    val videos by galleryViewModel.videos.collectAsState()
    val thumbnails = galleryViewModel.thumbnails
    val scope = rememberCoroutineScope()

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isSelectMode by remember { mutableStateOf(false) }
    val selectedPaths = remember { mutableListOf<String>().toMutableStateList() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSendScreen by remember { mutableStateOf(false) }
    var showReceiveScreen by remember { mutableStateOf(false) }

    fun exitSelectMode() {
        isSelectMode = false
        selectedPaths.clear()
        onSelectModeChanged(false)
    }

    LaunchedEffect(Unit) { galleryViewModel.loadVideos() }

    val bgColor = when (appearanceMode) {
        AppearanceMode.SIMPLE    -> BackgroundDark
        AppearanceMode.TACTICAL  -> Color(0xFF00CC44)
        AppearanceMode.MATCHA    -> Color(0xFF7BAF52)
        AppearanceMode.ICE_CREAM -> Color(0xFFFF8FAB)
        AppearanceMode.TROPICAL  -> Color(0xFFA0522D)
        AppearanceMode.SPIDER    -> Color(0xFFBB0000)
        else                     -> BackgroundDark
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (videos.isEmpty()) {
            EmptyGallery()
        } else {
            val totalBytes = remember(videos) { videos.sumOf { it.file.length() } }
            val totalSizeLabel = remember(totalBytes) {
                when {
                    totalBytes >= 1_073_741_824L -> "%.1f GB".format(totalBytes / 1_073_741_824.0)
                    totalBytes >= 1_048_576L     -> "%.1f MB".format(totalBytes / 1_048_576.0)
                    totalBytes >= 1_024L         -> "%.1f KB".format(totalBytes / 1_024.0)
                    else                         -> "$totalBytes B"
                }
            }
            var infoExpanded by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().navigationBarsPadding().padding(bottom = if (isSelectMode) 80.dp else 72.dp)) {
                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "Gallery ↓" tappable group
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { if (!isSelectMode) infoExpanded = !infoExpanded }
                    ) {
                        Text(
                            text       = "Gallery",
                            color      = Color.White,
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (infoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (isSelectMode) {
                        if (selectedPaths.isNotEmpty()) {
                            Text("${selectedPaths.size} selected", color = Color.White.copy(0.7f), fontSize = 13.sp)
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            "Cancel",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(Color.Black, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .clickable { exitSelectMode() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = "Receive via WiFi",
                                tint = Color.White.copy(0.7f),
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable { showReceiveScreen = true }
                            )
                            Text(
                                "Select",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .background(Color.Black, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                    .clickable {
                                        isSelectMode = true
                                        onSelectModeChanged(true)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = infoExpanded && !isSelectMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.White.copy(0.5f), modifier = Modifier.size(13.dp))
                            Text("${videos.size} files", color = Color.White.copy(0.65f), fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White.copy(0.5f), modifier = Modifier.size(13.dp))
                            Text(totalSizeLabel, color = Color.White.copy(0.65f), fontSize = 11.sp)
                        }
                    }
                }
            val gridSpacing = if (galleryColumns >= 3) 3.dp else 12.dp
            LazyVerticalGrid(
                columns = GridCells.Fixed(galleryColumns),
                contentPadding = PaddingValues(gridSpacing),
                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                itemsIndexed(videos, key = { _, v -> v.id }) { index, video ->
                    val path = video.file.absolutePath
                    LaunchedEffect(path) {
                        galleryViewModel.loadThumbnail(path)
                        if (!video.isPhoto) galleryViewModel.loadDuration(path)
                    }

                    VideoThumbnailCard(
                        video = video,
                        thumbnail = thumbnails[path],
                        durationMs = galleryViewModel.durations[path] ?: 0L,
                        onClick = { selectedIndex = index },
                        onDelete = { galleryViewModel.deleteVideo(video) },
                        showTimestamp = showTimestamp,
                        columns = galleryColumns,
                        isSelectMode = isSelectMode,
                        isSelected = selectedPaths.contains(path),
                        onToggleSelect = {
                            if (selectedPaths.contains(path)) selectedPaths.remove(path)
                            else selectedPaths.add(path)
                        }
                    )
                }
            } // end LazyVerticalGrid

            // Disclaimer
            Text(
                text       = "Photos and videos may be lost due to bugs or device issues.\nBack up important footage.",
                color      = Color.White.copy(alpha = 0.8f),
                fontSize   = 7.7.sp,
                textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 11.sp,
                modifier   = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)
            )
            } // end Column

        // ── Select mode bottom action bar ────────────────────────────────
        if (isSelectMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (selectedPaths.isNotEmpty()) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f),
                            androidx.compose.foundation.shape.CircleShape
                        )
                        .border(1.dp, Color.White.copy(alpha = if (selectedPaths.isNotEmpty()) 0.4f else 0.15f), androidx.compose.foundation.shape.CircleShape)
                        .clickable(enabled = selectedPaths.isNotEmpty()) {
                            val uris = ArrayList<Uri>(selectedPaths.map { path ->
                                val file = java.io.File(path)
                                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            })
                            val mime = if (selectedPaths.all { it.endsWith(".jpg") }) "image/*"
                                       else if (selectedPaths.all { it.endsWith(".mp4") }) "video/*"
                                       else "*/*"
                            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = mime
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share"))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.IosShare,
                        contentDescription = "Share",
                        tint = if (selectedPaths.isNotEmpty()) Color.White else Color.White.copy(0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (selectedPaths.isNotEmpty()) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.07f),
                            androidx.compose.foundation.shape.CircleShape
                        )
                        .border(1.dp, Color.White.copy(alpha = if (selectedPaths.isNotEmpty()) 0.4f else 0.15f), androidx.compose.foundation.shape.CircleShape)
                        .clickable(enabled = selectedPaths.isNotEmpty()) {
                            showSendScreen = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = "Send via WiFi",
                        tint = if (selectedPaths.isNotEmpty()) Color.White else Color.White.copy(0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (selectedPaths.isNotEmpty()) RecordRed.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.07f),
                            androidx.compose.foundation.shape.CircleShape
                        )
                        .border(1.dp, if (selectedPaths.isNotEmpty()) RecordRed else Color.White.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape)
                        .clickable(enabled = selectedPaths.isNotEmpty()) {
                            showDeleteConfirm = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = if (selectedPaths.isNotEmpty()) Color.White else Color.White.copy(0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        }
    }

    // ── Delete confirm dialog ─────────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selectedPaths.size} item${if (selectedPaths.size > 1) "s" else ""}?", color = TextPrimary) },
            text = { Text("This cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val toDelete = selectedPaths.toList()
                    exitSelectMode()
                    toDelete.forEach { path ->
                        videos.find { it.file.absolutePath == path }
                            ?.let { galleryViewModel.deleteVideo(it) }
                    }
                }) { Text("Delete", color = RecordRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = SurfaceDark
        )
    }

    // WiFi transfer overlays
    if (showSendScreen) {
        val filesToSend = remember(selectedPaths.toList()) {
            selectedPaths.mapNotNull { path -> java.io.File(path).takeIf { it.exists() } }
        }
        SendScreen(files = filesToSend, onDismiss = { showSendScreen = false })
    }
    if (showReceiveScreen) {
        ReceiveScreen(
            onDismiss = { showReceiveScreen = false },
            onFilesReceived = { galleryViewModel.loadVideos() }
        )
    }

    // Swipeable viewer sheet
    if (selectedIndex != null && videos.isNotEmpty()) {
        val safeIndex = selectedIndex!!.coerceIn(0, videos.lastIndex)
        val pagerState = rememberPagerState(initialPage = safeIndex) { videos.size }

        ModalBottomSheet(
            onDismissRequest = { selectedIndex = null },
            sheetState = sheetState,
            containerColor = SurfaceDark
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val item = videos.getOrNull(page) ?: return@HorizontalPager
                val isCurrentPage = pagerState.currentPage == page

                if (item.isPhoto) {
                    PhotoViewerScreen(
                        photo = item,
                        bitmap = thumbnails[item.file.absolutePath],
                        subscriptionManager = subscriptionManager,
                        onDismiss = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { selectedIndex = null }
                        },
                        onDeleted = {
                            galleryViewModel.deleteVideo(item)
                            selectedIndex = null
                        }
                    )
                } else {
                    VideoPlayerScreen(
                        video = item,
                        isActive = isCurrentPage,
                        subscriptionManager = subscriptionManager,
                        onDismiss = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { selectedIndex = null }
                        },
                        onDeleted = {
                            galleryViewModel.deleteVideo(item)
                            selectedIndex = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGallery() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = BorderColor,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No recordings yet",
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Recordings will appear here",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
    }
}
