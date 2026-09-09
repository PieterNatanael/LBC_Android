package com.example.bodycamoutdoorgear.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    subscriptionManager: SubscriptionManager,
    galleryViewModel: GalleryViewModel = viewModel()
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val appearanceMode by settingsViewModel.appearanceMode.collectAsState()
    val showTimestamp  by settingsViewModel.showGalleryTimestamp.collectAsState()
    val isAlt = appearanceMode != AppearanceMode.NORMAL

    val videos by galleryViewModel.videos.collectAsState()
    val thumbnails = galleryViewModel.thumbnails  // SnapshotStateMap — no collectAsState needed
    val scope = rememberCoroutineScope()

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { galleryViewModel.loadVideos() }

    val bgColor = when (appearanceMode) {
        AppearanceMode.SIMPLE    -> Color(0xFFCC0000)
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
            var infoExpanded by remember { mutableStateOf(true) }

            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
                // ── Header + collapsible info ─────────────────────────────────
                Box(
                    modifier = Modifier.fillMaxWidth().weight(15f),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, bottom = 6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text       = "Gallery",
                                color      = Color.White,
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(
                                imageVector = if (infoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp).clickable { infoExpanded = !infoExpanded }
                            )
                        }
                        AnimatedVisibility(visible = infoExpanded) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(top = 4.dp)
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
                    }
                }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().weight(50f)
            ) {
                itemsIndexed(videos, key = { _, v -> v.id }) { index, video ->
                    val path = video.file.absolutePath
                    LaunchedEffect(path) { galleryViewModel.loadThumbnail(path) }

                    VideoThumbnailCard(
                        video = video,
                        thumbnail = thumbnails[path],
                        durationMs = 0L,
                        onClick = { selectedIndex = index },
                        onDelete = { galleryViewModel.deleteVideo(video) },
                        showTimestamp = showTimestamp
                    )
                }
            } // end LazyVerticalGrid

            // Disclaimer — sits between the grid and the bottom of the column
            Box(
                modifier = Modifier.fillMaxWidth().weight(10f).padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text          = "Photos and videos may be lost due to bugs or device issues.\nBack up important footage.",
                    color         = Color.White.copy(alpha = 0.8f),
                    fontSize      = 7.7.sp,
                    textAlign     = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight    = 11.sp,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
            } // end Column
        }
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
