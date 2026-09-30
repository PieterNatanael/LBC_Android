package com.example.bodycamoutdoorgear.ui.gallery

import android.content.Intent
import com.example.bodycamoutdoorgear.ui.theme.themedShape
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.ui.draw.clip
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
import androidx.compose.runtime.DisposableEffect
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
import com.example.bodycamoutdoorgear.privategallery.PrivateGalleryStore
import com.example.bodycamoutdoorgear.viewmodel.AppearanceMode
import com.example.bodycamoutdoorgear.viewmodel.SettingsViewModel
import com.example.bodycamoutdoorgear.ui.player.PhotoViewerScreen
import com.example.bodycamoutdoorgear.ui.player.VideoPlayerScreen
import androidx.media3.exoplayer.ExoPlayer
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.RecordRed
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary
import com.example.bodycamoutdoorgear.viewmodel.GalleryViewModel
import com.example.bodycamoutdoorgear.ui.transfer.ReceiveScreen
import com.example.bodycamoutdoorgear.ui.transfer.SendScreen
import com.example.bodycamoutdoorgear.ui.paywall.PaywallScreen
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapVert
import com.example.bodycamoutdoorgear.viewmodel.GallerySort
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

// The "photos and videos may be lost" footer is hidden for now, since the same
// warning is already shown once at first launch. Flip to true to bring it back.
private const val SHOW_DATA_LOSS_FOOTER = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    subscriptionManager: SubscriptionManager,
    onSelectModeChanged: (Boolean) -> Unit = {},
    // Which folder this instance shows — Documents-equivalent (filesDir)
    // normally, or Private Gallery's own subfolder. isPrivateGallery flips
    // the header text and the dropdown's cross-link direction; the two
    // callbacks are how this instance asks its caller (AppNavigation) to
    // either start the PIN flow or close Private and go back, since this
    // screen has no PIN logic of its own.
    folder: java.io.File = LocalContext.current.filesDir,
    isPrivateGallery: Boolean = false,
    onOpenPrivateGallery: () -> Unit = {},
    onExitPrivateGallery: () -> Unit = {},
    // Bumped by the caller whenever this list may have changed behind its
    // back. The normal Gallery stays composed underneath Private, so its
    // LaunchedEffect never re-runs on its own when Private closes — a file
    // moved out of Private was missing here until something else reloaded.
    refreshKey: Int = 0,
    // A distinct key is required here: without it, Compose's viewModel()
    // resolves both the normal and the Private instance of this screen to
    // the SAME GalleryViewModel (this app has one Activity and no
    // NavBackStackEntry to scope them apart), and the two would fight over
    // which folder is currently loaded.
    galleryViewModel: GalleryViewModel = viewModel(key = if (isPrivateGallery) "private_gallery" else "normal_gallery")
) {
    val context = LocalContext.current
    LaunchedEffect(folder, refreshKey) { galleryViewModel.setFolder(folder) }
    val settingsViewModel: SettingsViewModel = viewModel()
    val appearanceMode by settingsViewModel.appearanceMode.collectAsState()
    val showTimestamp  by settingsViewModel.showGalleryTimestamp.collectAsState()
    val galleryColumns by settingsViewModel.galleryColumns.collectAsState()
    val gallerySort    by settingsViewModel.gallerySort.collectAsState()
    // The setting is shared, but each screen's ViewModel does its own
    // listing, so each is told when the choice changes.
    LaunchedEffect(gallerySort) { galleryViewModel.setSort(gallerySort) }
    val isAlt = appearanceMode != AppearanceMode.NORMAL

    val videos by galleryViewModel.videos.collectAsState()
    val thumbnails = galleryViewModel.thumbnails
    val scope = rememberCoroutineScope()

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isSelectMode by remember { mutableStateOf(false) }
    val selectedPaths = remember { mutableListOf<String>().toMutableStateList() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBulkMoveError by remember { mutableStateOf(false) }
    var bulkMoveFailureCount by remember { mutableStateOf(0) }
    var showSendScreen by remember { mutableStateOf(false) }
    var showReceiveScreen by remember { mutableStateOf(false) }
    val importProgress by galleryViewModel.importProgress.collectAsState()
    val importProblems by galleryViewModel.importProblems.collectAsState()
    // The system photo picker, which needs no storage permission: it hands
    // over only the items the user selects.
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> galleryViewModel.importMedia(uris) }
    val importFromPhotos = {
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }
    val isUnlocked = subscriptionManager.isUnlocked
    var showPaywall by remember { mutableStateOf(false) }

    fun exitSelectMode() {
        isSelectMode = false
        selectedPaths.clear()
        onSelectModeChanged(false)
    }

    val bgColor = when (appearanceMode) {
        AppearanceMode.SIMPLE    -> BackgroundDark
        AppearanceMode.TACTICAL  -> Color(0xFF00CC44)
        AppearanceMode.MATCHA    -> Color(0xFF7BAF52)
        AppearanceMode.ICE_CREAM -> Color(0xFFFF8FAB)
        AppearanceMode.TROPICAL  -> Color(0xFFA0522D)
        AppearanceMode.SPIDER    -> Color(0xFFBB0000)
        AppearanceMode.MARS      -> Color.Black
        else                     -> BackgroundDark
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (videos.isEmpty()) {
            // Private Gallery starts empty and stays that way until
            // something is moved in — EmptyGallery has to carry its own way
            // back out, since the header (and its cross-link) below only
            // renders in the non-empty branch.
            EmptyGallery(
                onFromPhotos = importFromPhotos,
                onHipHopClick = { showReceiveScreen = true },
                isPrivateGallery = isPrivateGallery,
                onExitPrivateGallery = onExitPrivateGallery
            )
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

            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().then(if (isSelectMode) Modifier.navigationBarsPadding().padding(bottom = 96.dp) else Modifier)) {
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
                            text       = if (isPrivateGallery) "Private" else "Gallery",
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
                                .background(Color.Black, themedShape(4.dp))
                                .clickable { exitSelectMode() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ImportMenuChip(
                                onFromPhotos = importFromPhotos,
                                onHipHop = { showReceiveScreen = true },
                                horizontalPadding = 10.dp,
                                verticalPadding = 4.dp
                            )
                            Text(
                                "Select",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .background(Color.Black, themedShape(4.dp))
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
                    Column(modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        Spacer(Modifier.height(8.dp))
                        SortRow(
                            selected = gallerySort,
                            onSelect = { settingsViewModel.setGallerySort(it) }
                        )
                        Spacer(Modifier.height(8.dp))
                        // The cross-link: "Private" from inside the normal
                        // gallery, "Gallery" from inside Private itself —
                        // leaving Private never asks for the PIN, only
                        // entering it does (see onOpenPrivateGallery's caller
                        // in AppNavigation, which decides setup vs. unlock).
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable {
                                if (isPrivateGallery) onExitPrivateGallery() else onOpenPrivateGallery()
                            }
                        ) {
                            // Only .normal gets an icon here — leaving
                            // Private is meant to read as a plain,
                            // unremarkable "back to Gallery" label, not
                            // another call-to-action competing with
                            // "PRIVATE" above.
                            if (!isPrivateGallery) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White.copy(0.5f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                if (isPrivateGallery) "GALLERY" else "PRIVATE",
                                color = Color.White.copy(0.65f),
                                // Bigger specifically for "GALLERY" — the
                                // only way back out of Private, so it earns
                                // being a little more prominent than the
                                // "PRIVATE" link going the other way.
                                fontSize = if (isPrivateGallery) 14.sp else 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            val gridSpacing = if (galleryColumns >= 3) 3.dp else 12.dp
            // Thumbnails run behind the floating tab pill, so the last row needs extra
            // room to scroll up clear of it (select mode keeps its own bar padding).
            val navBottom = androidx.compose.foundation.layout.WindowInsets.navigationBars
                .asPaddingValues().calculateBottomPadding()
            val gridBottomExtra = if (isSelectMode) 0.dp else navBottom + 84.dp
            LazyVerticalGrid(
                columns = GridCells.Fixed(galleryColumns),
                contentPadding = PaddingValues(
                    start = gridSpacing, top = gridSpacing, end = gridSpacing,
                    bottom = gridSpacing + gridBottomExtra
                ),
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
            if (SHOW_DATA_LOSS_FOOTER) Text(
                text       = "Photos and videos may be lost due to bugs or device issues.\nBack up important footage.",
                color      = Color.White.copy(alpha = 0.8f),
                fontSize   = 7.7.sp,
                textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 11.sp,
                modifier   = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)
            )
            } // end Column

        // ── Bottom fade: thumbnails dissolve into the background behind the tab pill ──
        if (!isSelectMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(
                        androidx.compose.foundation.layout.WindowInsets.navigationBars
                            .asPaddingValues().calculateBottomPadding() + 140.dp
                    )
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, bgColor.copy(alpha = 0.85f), bgColor)
                        )
                    )
            )
        }

        // ── Select mode bottom action bar ────────────────────────────────
        if (isSelectMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.75f),
                        androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .navigationBarsPadding()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectActionButton(
                    icon = Icons.Default.IosShare,
                    label = "Share",
                    enabled = selectedPaths.isNotEmpty(),
                    onClick = {
                        if (!isUnlocked) {
                            showPaywall = true
                        } else {
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
                        }
                    }
                )
                SelectActionButton(
                    icon = Icons.Default.Wifi,
                    label = "HipHop",
                    enabled = selectedPaths.isNotEmpty(),
                    onClick = { showSendScreen = true }
                )
                // Bulk counterpart of each file's own Set as Private/Show in
                // Gallery button — same underlying moves, same lack of a PIN
                // check going in (adding to Private has never required
                // proving the PIN, only opening Private to look at it does).
                SelectActionButton(
                    icon = if (isPrivateGallery) Icons.Default.LockOpen else Icons.Default.Lock,
                    label = if (isPrivateGallery) "Gallery" else "Private",
                    enabled = selectedPaths.isNotEmpty(),
                    onClick = {
                        var failures = 0
                        selectedPaths.toList().forEach { path ->
                            try {
                                val file = java.io.File(path)
                                if (isPrivateGallery) PrivateGalleryStore.moveOut(context, file)
                                else PrivateGalleryStore.moveIn(context, file)
                            } catch (e: Exception) {
                                failures++
                            }
                        }
                        galleryViewModel.loadVideos()
                        exitSelectMode()
                        if (failures > 0) {
                            bulkMoveFailureCount = failures
                            showBulkMoveError = true
                        }
                    }
                )
                SelectActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    enabled = selectedPaths.isNotEmpty(),
                    activeColor = RecordRed,
                    onClick = { showDeleteConfirm = true }
                )
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

    if (showBulkMoveError) {
        AlertDialog(
            onDismissRequest = { showBulkMoveError = false },
            title = { Text("Some files couldn't be moved", color = TextPrimary) },
            text = { Text("$bulkMoveFailureCount of the selected files could not be moved. The rest moved successfully.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showBulkMoveError = false }) { Text("OK", color = TextPrimary) }
            },
            containerColor = SurfaceDark
        )
    }

    // A modal card, so nothing here can be tapped while files are still being
    // copied. Not dismissable: the copy can't be cancelled part way.
    importProgress?.let { (current, total) ->
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(SurfaceDark, themedShape(12.dp))
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "Importing $current of $total",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Only speaks up when something didn't make it.
    importProblems?.let { result ->
        AlertDialog(
            onDismissRequest = { galleryViewModel.dismissImportProblems() },
            title = {
                Text(if (result.noSpace > 0) "Not Enough Storage" else "Import", color = TextPrimary)
            },
            text = {
                Text(
                    if (result.noSpace > 0)
                        "${result.imported} imported. The rest need more free space. Free up storage and try again."
                    else
                        "${result.imported} imported, ${result.failed} couldn't be imported.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { galleryViewModel.dismissImportProblems() }) {
                    Text("OK", color = TextPrimary)
                }
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
            onFilesReceived = { galleryViewModel.loadVideos() },
            // Whichever folder THIS instance is showing — filesDir itself,
            // or Private Gallery's own subfolder. Without this, HipHop
            // receive always landed in the normal gallery even while
            // looking at Private, so a file received there silently didn't
            // show up on screen at all.
            saveDir = folder
        )
    }
    if (showPaywall) {
        PaywallScreen(subscriptionManager = subscriptionManager, onDismiss = { showPaywall = false })
    }

    // Swipeable viewer sheet
    if (selectedIndex != null && videos.isNotEmpty()) {
        val safeIndex = selectedIndex!!.coerceIn(0, videos.lastIndex)
        val pagerState = rememberPagerState(initialPage = safeIndex) { videos.size }

        // One ExoPlayer shared across every page of this viewer session — each page
        // used to create its own player (and hardware decoder), and swiping through
        // several videos without the old ones being torn down fast enough exhausted
        // decoder/heap resources and crashed with OutOfMemoryError. A single reused
        // player, with only the current page attaching it to its PlayerView, avoids
        // that entirely: there's only ever one decoder alive for this screen.
        val sharedVideoPlayer = remember { ExoPlayer.Builder(context).build() }
        DisposableEffect(Unit) {
            onDispose { sharedVideoPlayer.release() }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedIndex = null },
            sheetState = sheetState,
            containerColor = SurfaceDark
        ) {
            HorizontalPager(
                state = pagerState,
                key = { page -> videos.getOrNull(page)?.id ?: page },
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val item = videos.getOrNull(page) ?: return@HorizontalPager
                val isCurrentPage = pagerState.currentPage == page

                LaunchedEffect(item.file.absolutePath) {
                    galleryViewModel.loadThumbnail(item.file.absolutePath)
                }

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
                        },
                        onMoved = {
                            // The file itself already moved — this instance
                            // just needs to stop showing it as if it were
                            // still here. galleryViewModel.loadVideos()
                            // re-reads the folder it's already scoped to,
                            // which now correctly excludes it.
                            galleryViewModel.loadVideos()
                            selectedIndex = null
                        }
                    )
                } else {
                    VideoPlayerScreen(
                        video = item,
                        isActive = isCurrentPage,
                        exoPlayer = sharedVideoPlayer,
                        subscriptionManager = subscriptionManager,
                        onDismiss = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { selectedIndex = null }
                        },
                        onDeleted = {
                            galleryViewModel.deleteVideo(item)
                            selectedIndex = null
                        },
                        onMoved = {
                            galleryViewModel.loadVideos()
                            selectedIndex = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    activeColor: Color = Color.White
) {
    val tint = if (enabled) activeColor else Color.White.copy(alpha = 0.3f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(1.dp))
        Text(
            label,
            color = tint,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
            letterSpacing = 0.2.sp,
            maxLines = 1
        )
    }
}

/** The dropdown's sort choice, styled like the file count and size beside it. */
@Composable
private fun SortRow(selected: GallerySort, onSelect: (GallerySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Icon(Icons.Default.SwapVert, contentDescription = null, tint = Color.White.copy(0.5f), modifier = Modifier.size(13.dp))
            Text(selected.label, color = Color.White.copy(0.65f), fontSize = 11.sp)
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Color.White.copy(0.5f), modifier = Modifier.size(13.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GallerySort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    // The checkmark only marks the current choice.
                    trailingIcon = {
                        if (option == selected) Icon(Icons.Default.Check, contentDescription = null)
                    },
                    onClick = { expanded = false; onSelect(option) }
                )
            }
        }
    }
}

/**
 * One "Import" chip instead of a bare "HipHop": that name means nothing to
 * someone who hasn't used it yet, while "Import" says what the button is for.
 * HipHop keeps its name as a row inside the menu.
 */
@Composable
private fun ImportMenuChip(
    onFromPhotos: () -> Unit,
    onHipHop: () -> Unit,
    horizontalPadding: Dp,
    verticalPadding: Dp
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.Black, themedShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            Text("Import", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("From Photos") },
                leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                onClick = { expanded = false; onFromPhotos() }
            )
            DropdownMenuItem(
                text = { Text("HipHop") },
                leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) },
                onClick = { expanded = false; onHipHop() }
            )
        }
    }
}

@Composable
private fun EmptyGallery(
    onFromPhotos: () -> Unit,
    onHipHopClick: () -> Unit,
    isPrivateGallery: Boolean = false,
    onExitPrivateGallery: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isPrivateGallery) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .clickable(onClick = onExitPrivateGallery),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // No icon here either, matching the same cross-link in the
                // dropdown above — a plain, unremarkable "back to Gallery"
                // label.
                Text("GALLERY", color = Color.White.copy(0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isPrivateGallery) "Nothing here yet" else "No recordings yet",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isPrivateGallery) "Use \"Set as Private\" on a photo or video to move it here"
                       else "Recordings will appear here",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            // Shown for Private too, not just the normal gallery — reaching
            // this screen at all already required the PIN, so receiving
            // straight into an empty Private Gallery needs no extra gate.
            Spacer(Modifier.height(20.dp))
            ImportMenuChip(
                onFromPhotos = onFromPhotos,
                onHipHop = onHipHopClick,
                horizontalPadding = 14.dp,
                verticalPadding = 6.dp
            )
        }
    }
}
