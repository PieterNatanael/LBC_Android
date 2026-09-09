package com.example.bodycamoutdoorgear.ui.camera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.bodycamoutdoorgear.data.VideoQuality
import com.example.bodycamoutdoorgear.viewmodel.AppearanceMode
import com.example.bodycamoutdoorgear.viewmodel.DisplayMode
import com.example.bodycamoutdoorgear.viewmodel.QualityTier
import com.example.bodycamoutdoorgear.viewmodel.SettingsViewModel
import com.example.bodycamoutdoorgear.manager.SubscriptionManager
import com.example.bodycamoutdoorgear.ui.paywall.PaywallScreen
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.BorderColor
import com.example.bodycamoutdoorgear.ui.theme.ButtonFace
import com.example.bodycamoutdoorgear.ui.theme.CompassGold
import com.example.bodycamoutdoorgear.ui.theme.RecordRed
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary
import com.example.bodycamoutdoorgear.viewmodel.CameraViewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.compose.ui.graphics.Paint
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    subscriptionManager: SubscriptionManager,
    onRequestPermissions: () -> Unit,
    onShowIntro: () -> Unit = {},
    isActive: Boolean = true
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val isRecording      by viewModel.isRecording.collectAsState()
    val qualityTier      by settingsViewModel.videoQualityTier.collectAsState()
    val photoQualityTier by settingsViewModel.photoQualityTier.collectAsState()
    val gridEnabled      by settingsViewModel.gridEnabled.collectAsState()
    val appearanceMode   by settingsViewModel.appearanceMode.collectAsState()
    val displayMode      by settingsViewModel.displayMode.collectAsState()
    val isSaveBattery    = displayMode == DisplayMode.SAVE_BATTERY
    val isCircle         = displayMode == DisplayMode.CIRCLE
    val isSimple   = appearanceMode == AppearanceMode.SIMPLE
    val isTactical = appearanceMode == AppearanceMode.TACTICAL
    val isMatcha   = appearanceMode == AppearanceMode.MATCHA
    val isIceCream = appearanceMode == AppearanceMode.ICE_CREAM
    val isTropical = appearanceMode == AppearanceMode.TROPICAL
    val isSpider   = appearanceMode == AppearanceMode.SPIDER
    val isGradientBtn = isIceCream || isTropical || isSpider
    val isAlt = isSimple || isTactical || isMatcha || isIceCream || isTropical || isSpider
    val matchaGreen   = Color(0xFF7BAF52)
    val icePink       = Color(0xFFFF8FAB)
    val icePeach      = Color(0xFFFFB347)
    val woodBrown     = Color(0xFF6B3A2A)
    val coconutCream  = Color(0xFFA0522D)
    val spiderRed     = Color(0xFFBB0000)
    val spiderBlue    = Color(0xFF0033BB)
    val iceBrush      = Brush.linearGradient(listOf(icePink, icePeach))
    val tropicalBrush = Brush.linearGradient(listOf(woodBrown, coconutCream))
    val spiderBrush   = Brush.linearGradient(listOf(spiderRed, spiderBlue))
    val accentColor = when (appearanceMode) {
        AppearanceMode.SIMPLE    -> Color(0xFFCC0000)
        AppearanceMode.TACTICAL  -> Color(0xFF00CC44)
        AppearanceMode.MATCHA    -> matchaGreen
        AppearanceMode.ICE_CREAM -> icePink
        AppearanceMode.TROPICAL  -> coconutCream
        AppearanceMode.SPIDER    -> spiderRed
        else                     -> Color.White
    }
    val altBackground = if (isTactical) Color(0xFF0A0A0A) else Color.Black
    // SIMPLE uses pure black bg — boost glow to a vivid red so it's visible against black
    val glowColor = if (isSimple) Color(0xFFFF2020) else accentColor
    val matchaBg      = Color(0xFF0D1208)   // dark base — glow provides the color
    val iceCreamBg    = Color(0xFF110609)
    val tropicalBg    = Color(0xFF0E0803)
    val spiderBg      = Color(0xFF05050A)
    val isScreenDimmed   by viewModel.isScreenDimmed.collectAsState()
    val recordingError   by viewModel.recordingError.collectAsState()
    val isPreviewEnabled by viewModel.isPreviewEnabled.collectAsState()
    val isFrontCamera    by viewModel.isFrontCamera.collectAsState()

    fun checkPerms() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)       == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    var permsGranted by remember { mutableStateOf(checkPerms()) }

    // Re-check permissions when the Activity resumes (e.g. after the permission dialog closes)
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) permsGranted = checkPerms()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isSubscribed    by subscriptionManager.isSubscribed.collectAsState()
    val isGrandfathered by subscriptionManager.isGrandfathered.collectAsState()
    val isUnlocked = isSubscribed || isGrandfathered

    var showPaywall  by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Glow positioning — updated by onGloballyPositioned on the preview box
    val rootCoordsRef    = remember { arrayOfNulls<LayoutCoordinates>(1) }
    var glowCenter       by remember { mutableStateOf(Offset.Zero) }
    var glowHalfDiag     by remember { mutableStateOf(0f) }
    var previewBounds    by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }

    val cameraExecutor  = remember { Executors.newSingleThreadExecutor() }
    val previewView     = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    // Single movable composable for the camera preview — avoids "already has parent" crash
    // when switching between NORMAL (fullscreen) and alt (circle/rect) display modes.
    val cameraPreviewContent = remember {
        movableContentOf {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }
    }
    val providerRef     = remember { arrayOfNulls<ProcessCameraProvider>(1) }
    val previewRef      = remember { arrayOfNulls<Preview>(1) }
    var videoCapture    by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var cameraControl   by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var cameraInfo      by remember { mutableStateOf<androidx.camera.core.CameraInfo?>(null) }
    var focusPoint      by remember { mutableStateOf<Offset?>(null) }
    var zoomRatio       by remember { mutableStateOf(1f) }
    val initialRotation = remember { (context as? Activity)?.display?.rotation ?: android.view.Surface.ROTATION_0 }
    var targetRotation  by remember { mutableStateOf(initialRotation) }

    DisposableEffect(context) {
        val listener = object : android.view.OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                targetRotation = when {
                    orientation <= 45 || orientation > 315 -> android.view.Surface.ROTATION_0
                    orientation <= 135 -> android.view.Surface.ROTATION_270
                    orientation <= 225 -> android.view.Surface.ROTATION_180
                    else -> android.view.Surface.ROTATION_90
                }
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }

    LaunchedEffect(targetRotation, videoCapture) {
        videoCapture?.targetRotation = targetRotation
    }

    // Rebind camera on quality, facing, permission, or active-tab change
    LaunchedEffect(qualityTier, isFrontCamera, permsGranted, isActive) {
        if (!permsGranted || !isActive) {
            // Do NOT call unbindAll here — PhotoScreen's active bind also calls unbindAll,
            // and a concurrent unbindAll from the inactive screen would undo that bind.
            // The active screen always unbinds before rebinding (below), so this is safe.
            videoCapture = null
            return@LaunchedEffect
        }
        videoCapture = null  // clear before async bind so the record button stays disabled during setup
        val provider = suspendCoroutine<ProcessCameraProvider> { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
        }
        if (!isActive) return@LaunchedEffect  // screen left while waiting for provider
        providerRef[0] = provider
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        previewRef[0] = preview
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(settingsViewModel.videoQuality.cameraQuality))
            .setExecutor(cameraExecutor)
            .build()
        val vCapture = VideoCapture.withOutput(recorder)
        val selector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                       else CameraSelector.DEFAULT_BACK_CAMERA
        try {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, vCapture)
            videoCapture = vCapture
            cameraControl = camera.cameraControl
            cameraInfo = camera.cameraInfo
            zoomRatio = 1f
        } catch (e: Exception) {
            viewModel.setError("Camera error: ${e.message}")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.activeRecording?.stop()
            providerRef[0]?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    // Observe real zoom from hardware — keeps display in sync with hardware max
    DisposableEffect(cameraInfo) {
        val info = cameraInfo ?: return@DisposableEffect onDispose {}
        val observer = androidx.lifecycle.Observer<androidx.camera.core.ZoomState> { state ->
            zoomRatio = state.zoomRatio
        }
        info.zoomState.observeForever(observer)
        onDispose { info.zoomState.removeObserver(observer) }
    }

    // Screen brightness + preview pipeline on dim
    LaunchedEffect(isScreenDimmed) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val params = activity.window.attributes
        params.screenBrightness =
            if (isScreenDimmed) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = params

        // Skip preview unbind/rebind while recording to avoid session reconfiguration
        // artifacts in the recorded file — brightness change alone handles dim during recording
        if (!isRecording) {
            val provider = providerRef[0] ?: return@LaunchedEffect
            val preview  = previewRef[0]  ?: return@LaunchedEffect
            if (isScreenDimmed) {
                provider.unbind(preview)
            } else {
                val selector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                               else CameraSelector.DEFAULT_BACK_CAMERA
                try { provider.bindToLifecycle(lifecycleOwner, selector, preview) } catch (_: Exception) {}
            }
        }
    }

    // Keep screen on while recording
    LaunchedEffect(isRecording) {
        val activity = context as? Activity ?: return@LaunchedEffect
        if (isRecording) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            viewModel.setScreenDimmed(false)
        }
    }

    // ── Recording helpers ─────────────────────────────────────────────────────
    fun startRecording() {
        val vc = videoCapture ?: return
        if (!viewModel.hasEnoughStorage()) {
            viewModel.setError("Not enough storage. Free up space and try again.")
            return
        }
        val tmp  = File(context.cacheDir, "lb_tmp_${System.currentTimeMillis()}.mp4")
        val opts = FileOutputOptions.Builder(tmp).build()
        val rec  = vc.output.prepareRecording(context, opts)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start    -> viewModel.setRecording(true)
                    is VideoRecordEvent.Finalize -> {
                        viewModel.setRecording(false)
                        if (!event.hasError()) {
                            tmp.renameTo(File(context.filesDir, "video_${System.currentTimeMillis()}.mp4"))
                        } else {
                            tmp.delete()
                            viewModel.setError("Recording failed (error ${event.error})")
                        }
                    }
                    else -> {}
                }
            }
        viewModel.activeRecording = rec
    }

    fun stopRecording() {
        viewModel.activeRecording?.stop()
        viewModel.activeRecording = null
    }

    // Volume button hardware trigger
    LaunchedEffect(Unit) {
        viewModel.hardwareButtonEvent.collect {
            if (viewModel.isRecording.value) stopRecording() else startRecording()
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    Box(modifier = Modifier
        .fillMaxSize()
        .background(when {
            isMatcha   -> matchaBg
            isIceCream -> iceCreamBg
            isTropical -> tropicalBg
            isSpider   -> spiderBg
            isAlt      -> altBackground
            else       -> BackgroundDark
        })
        .onGloballyPositioned { rootCoordsRef[0] = it }
    ) {

        // Glow + vignette
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pB = previewBounds
            if (pB.width > 0f) {
                val cx = pB.center.x
                val cy = pB.center.y
                val screenDiag = sqrt(size.width.pow(2f) + size.height.pow(2f))

                if (isCircle) {
                    // Radial gradient: transparent inside circle → accent at edge → soft fade to screen corners
                    val r = pB.width / 2f
                    val edgeFrac = (r / screenDiag).coerceIn(0f, 0.95f)
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f                            to Color.Transparent,
                                (edgeFrac * 0.92f)            to Color.Transparent,
                                edgeFrac                      to glowColor.copy(alpha = 0.65f),
                                (edgeFrac + 0.12f).coerceAtMost(1f) to glowColor.copy(alpha = 0.35f),
                                (edgeFrac + 0.30f).coerceAtMost(1f) to glowColor.copy(alpha = 0.12f),
                                1f                            to Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = screenDiag
                        )
                    )
                } else {
                    // Rect modes: glow starts at shortest preview edge and spreads to screen edges.
                    // No transparent center zone needed — the preview view covers anything underneath.
                    val shortEdgeFrac = (minOf(pB.width, pB.height) / 2f / screenDiag).coerceIn(0f, 0.90f)
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                shortEdgeFrac                      to glowColor.copy(alpha = 0.65f),
                                (shortEdgeFrac + 0.15f).coerceAtMost(1f) to glowColor.copy(alpha = 0.35f),
                                (shortEdgeFrac + 0.35f).coerceAtMost(1f) to glowColor.copy(alpha = 0.12f),
                                1f                                 to Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = screenDiag
                        )
                    )
                }
            }
        }

        if (!permsGranted) {
            PermissionRequest(onRequest = onRequestPermissions)
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // ── Display mode NORMAL: fullscreen preview behind toolbar/buttons ──
                val isNormalDisplay = !isCircle && !isSaveBattery
                if (isNormalDisplay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coords ->
                                val root = rootCoordsRef[0] ?: return@onGloballyPositioned
                                val bounds = root.localBoundingBoxOf(coords, clipBounds = false)
                                glowCenter   = bounds.center
                                glowHalfDiag = sqrt((bounds.width / 2f).pow(2f) + (bounds.height / 2f).pow(2f))
                                previewBounds = bounds
                            }
                            .pointerInput(Unit) {
                                detectTapGestures { tap ->
                                    focusPoint = tap
                                    val factory = previewView.meteringPointFactory
                                    val point = factory.createPoint(tap.x, tap.y)
                                    val action = androidx.camera.core.FocusMeteringAction.Builder(point).build()
                                    cameraControl?.startFocusAndMetering(action)
                                }
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    val newZoom = (zoomRatio * zoom).coerceAtLeast(1f)
                                    try { cameraControl?.setZoomRatio(newZoom) } catch (_: Exception) {}
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        cameraPreviewContent()
                        if (gridEnabled) { GridOverlay() }
                        focusPoint?.let { fp -> FocusRing(offset = fp, onFinished = { focusPoint = null }) }
                    }
                }

                // ── Column layout: toolbar + preview slot + controls ──────────
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isUnlocked) {
                            SubtleProChip(onClick = { showPaywall = true })
                        }
                    }

                    // Preview slot — only for CIRCLE and SAVE_BATTERY display modes
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isNormalDisplay) {
                            Box(
                                modifier = Modifier
                                    .then(when {
                                        isCircle      -> Modifier.fillMaxWidth(0.82f).aspectRatio(1f)
                                        isSaveBattery -> Modifier.fillMaxWidth(0.72f).fillMaxHeight(0.80f)
                                        else          -> Modifier.fillMaxSize()
                                    })
                                    .onGloballyPositioned { coords ->
                                        val root = rootCoordsRef[0] ?: return@onGloballyPositioned
                                        val bounds = root.localBoundingBoxOf(coords, clipBounds = false)
                                        glowCenter   = bounds.center
                                        glowHalfDiag = sqrt((bounds.width / 2f).pow(2f) + (bounds.height / 2f).pow(2f))
                                        previewBounds = bounds
                                    }
                                    .then(when {
                                        isCircle -> Modifier.clip(CircleShape)
                                        isAlt    -> Modifier.clip(RoundedCornerShape(0.dp))
                                        else     -> Modifier
                                    })
                                    .then(when {
                                        isCircle -> Modifier.border(2.dp, accentColor, CircleShape)
                                        isAlt    -> Modifier.border(2.dp, accentColor, RoundedCornerShape(0.dp))
                                        else     -> Modifier
                                    })
                                    .pointerInput(Unit) {
                                        detectTapGestures { tap ->
                                            focusPoint = tap
                                            val factory = previewView.meteringPointFactory
                                            val point = factory.createPoint(tap.x, tap.y)
                                            val action = androidx.camera.core.FocusMeteringAction.Builder(point).build()
                                            cameraControl?.startFocusAndMetering(action)
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, _, zoom, _ ->
                                            val newZoom = (zoomRatio * zoom).coerceAtLeast(1f)
                                            try { cameraControl?.setZoomRatio(newZoom) } catch (_: Exception) {}
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                cameraPreviewContent()
                                if (gridEnabled) { GridOverlay() }
                                if (isSpider) { SpiderWebCorners() }
                                focusPoint?.let { fp -> FocusRing(offset = fp, onFinished = { focusPoint = null }) }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Recording timer badge — invisible when idle ──
                    Box(Modifier.alpha(if (isRecording) 1f else 0f)) { RecTimerBadge(isRecording) }
                    Spacer(Modifier.height(6.dp))
                    Spacer(Modifier.height(10.dp))

                // ── Record button + Flip/Dim left + Settings right ────────────
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGradientBtn) {
                        GradientRecordButton(
                            isRecording = isRecording,
                            gradientBrush = when {
                                isTropical -> tropicalBrush
                                isSpider   -> spiderBrush
                                else       -> iceBrush
                            },
                            onClick = { if (isRecording) stopRecording() else startRecording() }
                        )
                    } else if (isAlt) {
                        BauhausRecordButton(
                            isRecording = isRecording,
                            accentColor = accentColor,
                            innerRatio = if (isMatcha) 0.65f else 0.38f,
                            onClick = { if (isRecording) stopRecording() else startRecording() }
                        )
                    } else {
                        KnurledRecordButton(
                            isRecording = isRecording,
                            onClick = { if (isRecording) stopRecording() else startRecording() }
                        )
                    }
                    // Left: flip + dim buttons
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(SurfaceDark, CircleShape)
                                .clickable(enabled = !isRecording) { viewModel.flipCamera() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Cameraswitch,
                                contentDescription = "Flip camera",
                                tint = if (isRecording) accentColor.copy(0.3f) else if (isAlt) accentColor else Color.White,
                                modifier = Modifier.size(if (isAlt) 26.dp else 22.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(if (isScreenDimmed) SurfaceDark.copy(alpha = 0.5f) else SurfaceDark, CircleShape)
                                .clickable { viewModel.setScreenDimmed(!isScreenDimmed) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DarkMode,
                                contentDescription = "Dim screen",
                                tint = if (isScreenDimmed) accentColor.copy(0.3f) else if (isAlt) accentColor else Color.White,
                                modifier = Modifier.size(if (isAlt) 26.dp else 22.dp)
                            )
                        }
                    }
                    // Right: zoom + settings
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(SurfaceDark, CircleShape)
                                .clickable {
                                    zoomRatio = 1f
                                    cameraControl?.setZoomRatio(1f)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (zoomRatio == 1f) "1×" else "%.1f×".format(zoomRatio),
                                color = if (isAlt) accentColor else Color.White.copy(0.85f),
                                fontSize = if (isAlt) 14.sp else 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(SurfaceDark, CircleShape)
                                .clickable { showSettings = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (isAlt) accentColor else Color.White,
                                modifier = Modifier.size(if (isAlt) 26.dp else 22.dp)
                            )
                        }
                    }
                }

                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    Spacer(Modifier.height((LocalConfiguration.current.screenHeightDp * 0.16f).dp.coerceAtLeast(100.dp)))
                } // end Column layout

                // ── Full-screen dim overlay ───────────────────────────────────────
                if (isScreenDimmed) {
                    DimOverlay(onWake = { viewModel.setScreenDimmed(false) })
                }
            } // end content Box
        }
    }

    // Error dialog
    if (recordingError != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Recording Error", color = TextPrimary) },
            text  = { Text(recordingError ?: "", color = TextSecondary) },
            confirmButton = { Button(onClick = viewModel::clearError) { Text("OK") } },
            containerColor = SurfaceDark
        )
    }

    if (showPaywall) {
        PaywallScreen(
            subscriptionManager = subscriptionManager,
            onDismiss = { showPaywall = false }
        )
    }

    // Settings bottom sheet
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "SETTINGS",
                    color = Color.White.copy(0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )

                // Appearance
                Text(
                    "APPEARANCE",
                    color = Color.White.copy(0.4f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                val allModes = AppearanceMode.entries
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(allModes.take(3), allModes.drop(3)).forEach { rowModes ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowModes.forEach { mode ->
                                val selected = mode == appearanceMode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) Color.White.copy(0.25f) else Color.White.copy(0.1f))
                                        .border(1.dp, if (selected) Color.White.copy(0.5f) else Color.White.copy(0.2f), RoundedCornerShape(6.dp))
                                        .clickable { settingsViewModel.setAppearanceMode(mode) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(mode.label, color = if (selected) Color.White else Color.White.copy(0.4f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }
                }

                // Display Mode
                Text(
                    "DISPLAY MODE",
                    color = Color.White.copy(0.4f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DisplayMode.entries.forEach { mode ->
                        val selected = mode == displayMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color.White.copy(0.25f) else Color.White.copy(0.1f))
                                .border(1.dp, if (selected) Color.White.copy(0.5f) else Color.White.copy(0.2f), RoundedCornerShape(6.dp))
                                .clickable { settingsViewModel.setDisplayMode(mode) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mode.label, color = if (selected) Color.White else Color.White.copy(0.4f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                // Video Quality
                Text(
                    "VIDEO QUALITY",
                    color = Color.White.copy(0.4f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QualityTier.entries.forEach { tier ->
                        val selected = tier == qualityTier
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color.White.copy(0.25f) else Color.White.copy(0.1f))
                                .border(1.dp, if (selected) Color.White.copy(0.5f) else Color.White.copy(0.2f), RoundedCornerShape(6.dp))
                                .clickable { settingsViewModel.setVideoQualityTier(tier) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tier.shortLabel, color = if (selected) Color.White else Color.White.copy(0.4f), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                // Photo Quality
                Text(
                    "PHOTO QUALITY",
                    color = Color.White.copy(0.4f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QualityTier.entries.forEach { tier ->
                        val selected = tier == photoQualityTier
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color.White.copy(0.25f) else Color.White.copy(0.1f))
                                .border(1.dp, if (selected) Color.White.copy(0.5f) else Color.White.copy(0.2f), RoundedCornerShape(6.dp))
                                .clickable { settingsViewModel.setPhotoQualityTier(tier) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tier.shortLabel, color = if (selected) Color.White else Color.White.copy(0.4f), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                // Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GRID", color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    androidx.compose.material3.Switch(
                        checked = gridEnabled,
                        onCheckedChange = { settingsViewModel.toggleGrid() }
                    )
                }

                // Gallery timestamp
                val showGalleryTimestamp by settingsViewModel.showGalleryTimestamp.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GALLERY DATE", color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    androidx.compose.material3.Switch(
                        checked = showGalleryTimestamp,
                        onCheckedChange = { settingsViewModel.toggleGalleryTimestamp() }
                    )
                }

                // Gallery columns
                Text(
                    "GALLERY COLUMNS",
                    color = Color.White.copy(0.4f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                val galleryColumns by settingsViewModel.galleryColumns.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 3).forEach { cols ->
                        val selected = cols == galleryColumns
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color.White.copy(0.25f) else Color.White.copy(0.1f))
                                .border(1.dp, if (selected) Color.White.copy(0.5f) else Color.White.copy(0.2f), RoundedCornerShape(6.dp))
                                .clickable { settingsViewModel.setGalleryColumns(cols) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$cols", color = if (selected) Color.White else Color.White.copy(0.4f), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                // View Intro Again
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(0.08f))
                        .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(6.dp))
                        .clickable {
                            settingsViewModel.resetIntro()
                            showSettings = false
                            onShowIntro()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "VIEW INTRO AGAIN",
                        color = Color.White.copy(0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Knurled Record Button ─────────────────────────────────────────────────────

@Composable
fun KnurledRecordButton(isRecording: Boolean, onClick: () -> Unit, size: Dp = 100.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Tick marks drawn on canvas
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            for (i in 0 until 36) {
                val isMajor = i % 3 == 0
                val tickW = if (isMajor) 3.dp.toPx() else 2.dp.toPx()
                val tickH = if (isMajor) 10.dp.toPx() else 6.dp.toPx()
                val alpha = if (isMajor) 0.55f else 0.28f
                withTransform({ rotate(i * 10f, center) }) {
                    drawLine(
                        color = Color.White.copy(alpha = alpha),
                        start = Offset(center.x, 1.dp.toPx()),
                        end   = Offset(center.x, 1.dp.toPx() + tickH),
                        strokeWidth = tickW,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Outer metallic ring
        Box(
            modifier = Modifier
                .size(size * 0.88f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(0.38f), Color.White.copy(0.14f)),
                        center = Offset(0f, 0f),
                        radius = size.value * 2.5f
                    )
                )
                .border(1.dp, Color.White.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Inner button face
            Box(
                modifier = Modifier
                    .size(size * 0.7f)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) Brush.radialGradient(
                            colors = listOf(Color(0xFFD91F1F), Color(0xFF73060A)),
                            center = Offset(0f, 0f),
                            radius = size.value * 2f
                        ) else Brush.radialGradient(
                            colors = listOf(Color.White.copy(0.92f), Color.White.copy(0.65f)),
                            center = Offset(0f, 0f),
                            radius = size.value * 2f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    // White stop square
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(0.9f))
                    )
                } else {
                    // Red record dot
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD91A1A))
                    )
                }
            }
        }
    }
}

@Composable
fun GradientRecordButton(isRecording: Boolean, onClick: () -> Unit, gradientBrush: Brush, size: Dp = 70.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(width = 3.dp, color = Color.White, shape = CircleShape)
            .background(if (isRecording) gradientBrush else Brush.radialGradient(listOf(Color(0xFF1A0810), Color.Black)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Box(Modifier.size(size * 0.32f).background(Color.White))
        } else {
            Box(Modifier.size(size * 0.65f).clip(CircleShape).background(gradientBrush))
        }
    }
}

@Composable
fun BauhausRecordButton(isRecording: Boolean, onClick: () -> Unit, accentColor: Color = Color(0xFFCC0000), innerRatio: Float = 0.38f, size: Dp = 70.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(3.dp, Color.White, CircleShape)
            .background(if (isRecording) accentColor else Color.Black)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Box(Modifier.size(size * 0.32f).background(Color.White))
        } else {
            Box(Modifier.size(size * innerRatio).clip(CircleShape).background(accentColor))
        }
    }
}

// ── Supporting composables ────────────────────────────────────────────────────

@Composable
private fun PreviewToggleButton(isOn: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isOn) Color.White.copy(0.18f) else Color.White.copy(0.1f))
            .border(
                1.dp,
                if (isOn) Color.White.copy(0.45f) else Color.White.copy(0.2f),
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isOn) Icons.Default.Videocam else Icons.Default.Videocam,
            contentDescription = null,
            tint = if (isOn) Color.White.copy(0.8f) else Color.White.copy(0.4f),
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = if (isOn) "PREVIEW ON" else "PREVIEW OFF",
            color = if (isOn) Color.White.copy(0.8f) else Color.White.copy(0.4f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun RecTimerBadge(isRecording: Boolean) {
    var elapsedSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(isRecording) {
        elapsedSeconds = 0L
        if (isRecording) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                elapsedSeconds++
            }
        }
    }
    val h = elapsedSeconds / 3600
    val m = (elapsedSeconds % 3600) / 60
    val s = elapsedSeconds % 60
    val timeText = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(RecordRed)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White))
        Text(
            text       = timeText,
            color      = Color.White,
            fontSize   = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun DimScreenButton(isDimmed: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDimmed) Color.White.copy(0.04f) else Color.White.copy(0.12f))
            .border(
                1.dp,
                if (isDimmed) Color.White.copy(0.15f) else Color.White.copy(0.3f),
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.DarkMode,
            null,
            tint = if (isDimmed) Color.White.copy(0.3f) else Color.White.copy(0.7f),
            modifier = Modifier.size(13.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (isDimmed) "SCREEN DIMMED" else "DIM SCREEN",
                color = if (isDimmed) Color.White.copy(0.3f) else Color.White.copy(0.7f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                "saves battery for long recordings",
                color = if (isDimmed) Color.White.copy(0.25f) else Color.White.copy(0.38f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun DimOverlay(onWake: () -> Unit) {
    val alpha by rememberInfiniteTransition(label = "wake").animateFloat(
        initialValue = 0.18f,
        targetValue  = 0.05f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "wakeAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onWake),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.padding(bottom = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(Color.White.copy(alpha = alpha)))
            Text(
                "·  T A P  T O  W A K E  ·",
                color = Color.White.copy(0.08f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
internal fun SubtleProChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .border(1.dp, CompassGold.copy(alpha = 0.4f), RoundedCornerShape(5.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = "Upgrade to Pro",
            tint = CompassGold.copy(alpha = 0.7f),
            modifier = Modifier.size(10.dp)
        )
        Text(
            "PRO",
            color = CompassGold.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun GridOverlay() {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        val color = Color.White.copy(alpha = 0.35f)
        // 2 vertical lines
        drawLine(color, start = androidx.compose.ui.geometry.Offset(w / 3f, 0f), end = androidx.compose.ui.geometry.Offset(w / 3f, h), strokeWidth = 1.dp.toPx())
        drawLine(color, start = androidx.compose.ui.geometry.Offset(w * 2f / 3f, 0f), end = androidx.compose.ui.geometry.Offset(w * 2f / 3f, h), strokeWidth = 1.dp.toPx())
        // 2 horizontal lines
        drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, h / 3f), end = androidx.compose.ui.geometry.Offset(w, h / 3f), strokeWidth = 1.dp.toPx())
        drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, h * 2f / 3f), end = androidx.compose.ui.geometry.Offset(w, h * 2f / 3f), strokeWidth = 1.dp.toPx())
    }
}

@Composable
fun FocusRing(offset: Offset, onFinished: () -> Unit) {
    val alpha = remember { androidx.compose.animation.core.Animatable(1f) }
    val scale = remember { androidx.compose.animation.core.Animatable(1.4f) }
    LaunchedEffect(offset) {
        kotlinx.coroutines.coroutineScope {
            launch { scale.animateTo(1f, androidx.compose.animation.core.tween(300)) }
            launch {
                kotlinx.coroutines.delay(600)
                alpha.animateTo(0f, androidx.compose.animation.core.tween(300))
            }
        }
        onFinished()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = alpha.value),
                radius = 36.dp.toPx() * scale.value,
                center = offset,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun SpiderWebCorners() {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val webSize = 90.dp.toPx()
        val lineCount = 6
        val arcCount = 4
        val strokePx = 1.2.dp.toPx()
        val webColor = Color.White.copy(alpha = 0.65f)
        val style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx)

        // corners: (cx, cy, startAngle in deg, angleRange in deg)
        val corners = listOf(
            Offset(0f, 0f) to Pair(0f, 90f),
            Offset(size.width, 0f) to Pair(90f, 90f),
            Offset(size.width, size.height) to Pair(180f, 90f),
            Offset(0f, size.height) to Pair(270f, 90f)
        )

        for ((corner, angles) in corners) {
            val (startDeg, rangeDeg) = angles
            val startRad = Math.toRadians(startDeg.toDouble())
            val endRad   = Math.toRadians((startDeg + rangeDeg).toDouble())

            // Radial lines from corner
            for (i in 0..lineCount) {
                val t = i.toFloat() / lineCount.toFloat()
                val angle = startRad + t * (endRad - startRad)
                val endX = corner.x + cos(angle).toFloat() * webSize
                val endY = corner.y + sin(angle).toFloat() * webSize
                drawLine(
                    color = webColor,
                    start = corner,
                    end = Offset(endX, endY),
                    strokeWidth = strokePx,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            // Concentric arcs
            for (a in 1..arcCount) {
                val radius = webSize * a.toFloat() / arcCount.toFloat()
                val topLeft = Offset(corner.x - radius, corner.y - radius)
                drawArc(
                    color = webColor,
                    startAngle = startDeg,
                    sweepAngle = rangeDeg,
                    useCenter = false,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                    style = style
                )
            }
        }
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Camera & Microphone Access Required", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Low Battery Camera needs camera and microphone permissions to record.", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = ButtonFace)
            ) { Text("Grant Permissions", letterSpacing = 1.sp) }
        }
    }
}
