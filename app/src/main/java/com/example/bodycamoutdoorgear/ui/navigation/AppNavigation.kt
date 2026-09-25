package com.example.bodycamoutdoorgear.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodycamoutdoorgear.manager.SubscriptionManager
import com.example.bodycamoutdoorgear.privategallery.PrivateGalleryStore
import com.example.bodycamoutdoorgear.privategallery.PrivateGallerySession
import com.example.bodycamoutdoorgear.privategallery.findActivity
import com.example.bodycamoutdoorgear.ui.camera.CameraScreen
import com.example.bodycamoutdoorgear.ui.camera.PhotoScreen
import com.example.bodycamoutdoorgear.ui.gallery.GalleryScreen
import com.example.bodycamoutdoorgear.ui.gallery.PrivateGalleryLockMode
import com.example.bodycamoutdoorgear.ui.gallery.PrivateGalleryLockScreen
import com.example.bodycamoutdoorgear.viewmodel.AppearanceMode
import com.example.bodycamoutdoorgear.viewmodel.CameraViewModel
import com.example.bodycamoutdoorgear.viewmodel.PhotoViewModel
import com.example.bodycamoutdoorgear.viewmodel.SettingsViewModel
import androidx.compose.runtime.collectAsState

private sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Video   : Screen("video",   "Video",   Icons.Default.Videocam)
    object Photo   : Screen("photo",   "Photo",   Icons.Default.CameraAlt)
    object Gallery : Screen("gallery", "LBC",     RectangleStackIcon)
}

@Composable
fun AppNavigation(subscriptionManager: SubscriptionManager, onShowIntro: () -> Unit = {}) {
    val themeViewModel: SettingsViewModel = viewModel()
    val themeMode by themeViewModel.appearanceMode.collectAsState()
    val chrome: Color? = when (themeMode) {
        AppearanceMode.SIMPLE    -> Color(0xFFCC0000)
        AppearanceMode.TACTICAL  -> Color(0xFF00CC44)
        AppearanceMode.MATCHA    -> Color(0xFF7BAF52)
        AppearanceMode.ICE_CREAM -> Color(0xFFFF8FAB)
        AppearanceMode.TROPICAL  -> Color(0xFFF5F0DD)
        AppearanceMode.SPIDER    -> Color(0xFFDD2222)
        AppearanceMode.MARS      -> com.example.bodycamoutdoorgear.ui.theme.MarsRust
        else                     -> null
    }
    androidx.compose.runtime.CompositionLocalProvider(
        com.example.bodycamoutdoorgear.ui.theme.LocalMarsTheme provides (themeMode == AppearanceMode.MARS),
        com.example.bodycamoutdoorgear.ui.theme.LocalChromeAccent provides chrome
    ) {
        AppNavigationContent(subscriptionManager, onShowIntro)
    }
}

@Composable
private fun AppNavigationContent(subscriptionManager: SubscriptionManager, onShowIntro: () -> Unit) {
    val cameraViewModel: CameraViewModel   = viewModel()
    val photoViewModel: PhotoViewModel     = viewModel()
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
        else                     -> Color.White
    }

    val cameraScreenDimmed by cameraViewModel.isScreenDimmed.collectAsState()
    val photoScreenDimmed  by photoViewModel.isScreenDimmed.collectAsState()
    val isScreenDimmed = cameraScreenDimmed || photoScreenDimmed

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    var currentTab by rememberSaveable { mutableStateOf(Screen.Video.route) }
    var isGallerySelectMode by rememberSaveable { mutableStateOf(false) }

    // ── Private Gallery ──────────────────────────────────────────────────
    // Not rememberSaveable: neither should survive a process death the way
    // currentTab does — resuming straight back into an unlocked Private
    // Gallery after the OS killed and restored this Activity would defeat
    // the whole point of it relocking on backgrounding, below.
    var privateFlowMode by androidx.compose.runtime.remember { mutableStateOf<PrivateGalleryLockMode?>(null) }
    var showPrivateGallery by androidx.compose.runtime.remember { mutableStateOf(false) }
    val privateContext = LocalContext.current

    // Relocks on every backgrounding, not just while Private happens to be
    // open — cheap either way, and this is the one place that can't miss it
    // the way a per-screen effect could if Private wasn't the active screen
    // at the moment the app was backgrounded.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                PrivateGallerySession.lock()
                showPrivateGallery = false
                privateFlowMode = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // FLAG_SECURE for as long as either the PIN screen or Private Gallery
    // itself is showing — blocks screenshots and screen recording outright,
    // and blanks the recents-screen thumbnail, while either is on screen.
    // iOS has no equivalent API; this is genuinely stronger than what that
    // build could do, so there's no separate "screenshots aren't blocked"
    // notice needed here at all.
    val isPrivateFlowShowing = privateFlowMode != null || showPrivateGallery
    DisposableEffect(isPrivateFlowShowing) {
        val activity = privateContext.findActivity()
        if (isPrivateFlowShowing) {
            activity?.window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Camera screens: always in composition so the camera lifecycle is
        //    never torn down on tab switch. Hidden via graphicsLayer alpha when
        //    inactive; only the active screen binds the camera hardware.
        val isVideoActive = currentTab == Screen.Video.route
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (isVideoActive) 1f else 0f)
                .graphicsLayer { alpha = if (isVideoActive) 1f else 0f }
                .then(if (!isVideoActive) Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                                .changes.forEach { it.consume() }
                        }
                    }
                } else Modifier)
        ) {
            CameraScreen(
                viewModel            = cameraViewModel,
                subscriptionManager  = subscriptionManager,
                onRequestPermissions = {
                    permissionsLauncher.launch(
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    )
                },
                onShowIntro = onShowIntro,
                isActive    = currentTab == Screen.Video.route
            )
        }

        val isPhotoActive = currentTab == Screen.Photo.route
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (isPhotoActive) 1f else 0f)
                .graphicsLayer { alpha = if (isPhotoActive) 1f else 0f }
                .then(if (!isPhotoActive) Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                                .changes.forEach { it.consume() }
                        }
                    }
                } else Modifier)
        ) {
            PhotoScreen(
                viewModel            = photoViewModel,
                subscriptionManager  = subscriptionManager,
                onRequestPermissions = {
                    permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                },
                onShowIntro = onShowIntro,
                isActive    = currentTab == Screen.Photo.route
            )
        }

        // Gallery has no camera — safe to add/remove from composition.
        if (currentTab == Screen.Gallery.route) {
            Box(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                GalleryScreen(
                    subscriptionManager = subscriptionManager,
                    onSelectModeChanged = { isGallerySelectMode = it },
                    onOpenPrivateGallery = {
                        privateFlowMode = if (PrivateGalleryStore.isActive(privateContext))
                            PrivateGalleryLockMode.UNLOCK else PrivateGalleryLockMode.SETUP
                    }
                )
            }
        }

        // The PIN screen, then Private Gallery itself, both full-screen and
        // above everything else — mirrors how the normal Gallery box above
        // sits at zIndex 2f, just higher again.
        privateFlowMode?.let { mode ->
            Box(modifier = Modifier.fillMaxSize().zIndex(3f)) {
                PrivateGalleryLockScreen(
                    mode = mode,
                    onUnlocked = {
                        PrivateGallerySession.unlock()
                        privateFlowMode = null
                        showPrivateGallery = true
                    },
                    onDismiss = { privateFlowMode = null }
                )
            }
        }
        if (showPrivateGallery) {
            Box(modifier = Modifier.fillMaxSize().zIndex(3f)) {
                GalleryScreen(
                    subscriptionManager = subscriptionManager,
                    folder = PrivateGalleryStore.folder(privateContext),
                    isPrivateGallery = true,
                    onExitPrivateGallery = { showPrivateGallery = false }
                )
            }
        }

        // ── Floating liquid-glass pill — hidden when screen is dimmed or gallery select mode ──
        if (!isScreenDimmed && !isGallerySelectMode) {
            LiquidGlassPill(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
                    .zIndex(2f),
                currentTab  = currentTab,
                accentColor = accentColor,
                onNavigate  = { screen -> currentTab = screen.route }
            )
        }
    }
}

@Composable
private fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    currentTab: String,
    accentColor: Color = Color.White,
    onNavigate: (Screen) -> Unit
) {
    val screens  = listOf(Screen.Video, Screen.Photo, Screen.Gallery)
    val isModern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Nearly invisible body — the bright top rim carries the "glass" signal
    val pillBg = if (isModern)
        Brush.verticalGradient(listOf(Color.White.copy(0.14f), Color.White.copy(0.04f)))
    else
        Brush.verticalGradient(listOf(Color(0xFF2A2A2C).copy(0.85f), Color(0xFF111113).copy(0.82f)))

    // Strong specular highlight at top, fades to nothing at bottom
    val pillBorder = if (isModern)
        Brush.verticalGradient(listOf(Color.White.copy(0.72f), Color.White.copy(0.04f)))
    else
        Brush.verticalGradient(listOf(Color.White.copy(0.28f), Color.White.copy(0.04f)))

    Row(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(50), ambientColor = Color.Black.copy(0.6f), spotColor = Color.Black.copy(0.6f))
            .background(brush = pillBg, shape = RoundedCornerShape(50))
            .border(width = 1.dp, brush = pillBorder, shape = RoundedCornerShape(50))
            .padding(horizontal = 5.dp, vertical = 2.5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        screens.forEach { screen ->
            val selected = currentTab == screen.route
            PillTab(screen = screen, selected = selected, isModern = isModern) { onNavigate(screen) }
        }
    }
}

@Composable
private fun PillTab(
    screen: Screen,
    selected: Boolean,
    isModern: Boolean,
    onClick: () -> Unit
) {
    val targetBg = when {
        selected && isModern -> Color.White.copy(alpha = 0.90f)
        selected             -> Color.White.copy(alpha = 0.18f)
        else                 -> Color.Transparent
    }
    val targetContent = when {
        selected && isModern -> Color.Black
        selected             -> Color.White
        else                 -> Color.White
    }

    val bgColor      by animateColorAsState(targetBg,      tween(200), label = "tabBg")
    val contentColor by animateColorAsState(targetContent, tween(200), label = "tabContent")

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 25.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Icon(
                imageVector        = screen.icon,
                contentDescription = screen.label,
                tint               = contentColor,
                modifier           = Modifier.size(27.dp)
            )
            Text(
                text          = screen.label,
                fontSize      = 9.sp,
                lineHeight    = 9.sp,
                fontWeight    = FontWeight.Bold,
                fontFamily    = FontFamily.SansSerif,
                color         = contentColor,
                letterSpacing = 0.2.sp,
                modifier      = Modifier.offset(y = (-2).dp)
            )
        }
    }
}
