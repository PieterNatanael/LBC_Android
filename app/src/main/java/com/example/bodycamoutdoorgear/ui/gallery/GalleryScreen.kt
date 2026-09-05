package com.example.bodycamoutdoorgear.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
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
    val isAlt = appearanceMode != AppearanceMode.NORMAL

    val videos by galleryViewModel.videos.collectAsState()
    val thumbnails by galleryViewModel.thumbnails.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { galleryViewModel.loadVideos() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(when (appearanceMode) {
                AppearanceMode.MATCHA    -> Modifier.background(Brush.verticalGradient(listOf(Color(0xFF243318), Color(0xFF111A09), Color.Black)))
                AppearanceMode.ICE_CREAM -> Modifier.background(Brush.verticalGradient(listOf(Color(0xFF2D0F1F), Color(0xFF160810), Color.Black)))
                AppearanceMode.TACTICAL  -> Modifier.background(Color(0xFF0A0A0A))
                AppearanceMode.SIMPLE    -> Modifier.background(Color.Black)
                else                     -> Modifier.background(BackgroundDark)
            })
    ) {
        if (videos.isEmpty()) {
            EmptyGallery()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(videos, key = { _, v -> v.id }) { index, video ->
                    val path = video.file.absolutePath
                    LaunchedEffect(path) { galleryViewModel.loadThumbnail(path) }

                    VideoThumbnailCard(
                        video = video,
                        thumbnail = thumbnails[path],
                        durationMs = 0L,
                        onClick = { selectedIndex = index },
                        onDelete = {
                            galleryViewModel.deleteVideo(video)
                        }
                    )
                }
            }
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
