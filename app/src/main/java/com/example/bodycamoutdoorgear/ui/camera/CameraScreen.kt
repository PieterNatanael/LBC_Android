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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
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
    onShowIntro: () -> Unit = {}
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val isRecording      by viewModel.isRecording.collectAsState()
    val qualityTier      by settingsViewModel.videoQualityTier.collectAsState()
    val photoQualityTier by settingsViewModel.photoQualityTier.collectAsState()
    val gridEnabled      by settingsViewModel.gridEnabled.collectAsState()
    val appearanceMode   by settingsViewModel.appearanceMode.collectAsState()
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
    val woodBrown     = Color(0xFF8B5E3C)
    val coconutCream  = Color(0xFFF0D5A0)
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
    val matchaBg      = Color(0xFF0D1208)   // dark base — glow provides the color
    val iceCreamBg    = Color(0xFF110609)
    val tropicalBg    = Color(0xFF0E0803)
    val spiderBg      = Color(0xFF05050A)
    val isScreenDimmed   by viewModel.isScreenDimmed.collectAsState()
    val recordingError   by viewModel.recordingError.collectAsState()
    val isPreviewEnabled by viewModel.isPreviewEnabled.collectAsState()
    val isFrontCamera    by viewModel.isFrontCamera.collectAsState()

    val camPerm   = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)       == PackageManager.PERMISSION_GRANTED
    val audioPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val permsGranted = camPerm && audioPerm

    val isSubscribed    by subscriptionManager.isSubscribed.collectAsState()
    val isGrandfathered by subscriptionManager.isGrandfathered.collectAsState()
    val isUnlocked = isSubscribed || isGrandfathered

    var showPaywall  by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Glow positioning — updated by onGloballyPositioned on the preview box
    val rootCoordsRef    = remember { arrayOfNulls<LayoutCoordinates>(1) }
    var glowCenter       by remember { mutableStateOf(Offset.Zero) }
    var glowHalfDiag     by remember { mutableStateOf(0f) }

    val cameraExecutor  = remember { Executors.newSingleThreadExecutor() }
    val previewView     = remember { PreviewView(context) }
    val providerRef     = remember { arrayOfNulls<ProcessCameraProvider>(1) }
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

    // Rebind camera on quality, facing or permission change
    LaunchedEffect(qualityTier, isFrontCamera, permsGranted) {
        if (!permsGranted) return@LaunchedEffect
        val provider = suspendCoroutine<ProcessCameraProvider> { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
        }
        if (!isActive) return@LaunchedEffect  // screen left while waiting for provider
        providerRef[0] = provider
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
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

    // Screen brightness
    LaunchedEffect(isScreenDimmed) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val params = activity.window.attributes
        params.screenBrightness =
            if (isScreenDimmed) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = params
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

        // Radial glow — theme color at preview border, darkening outward to screen edges
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (glowHalfDiag > 0f) {
                val screenDiag = sqrt(size.width.pow(2f) + size.height.pow(2f)) / 2f
                val total = screenDiag * 1.05f
                val peak  = (glowHalfDiag / total).coerceIn(0.05f, 0.88f)
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f           to Color.Transparent,
                            peak * 0.88f to Color.Transparent,
                            peak         to accentColor.copy(alpha = 0.55f),
                            1f           to Color.Black.copy(alpha = 0.85f)
                        ),
                        center = glowCenter,
                        radius = total
                    )
                )
            }
        }

        if (!permsGranted) {
            PermissionRequest(onRequest = onRequestPermissions)
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Toolbar ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview toggle
                    // PRO chip
                    if (!isUnlocked) {
                        SubtleProChip(onClick = { showPaywall = true })
                    }
                }

                // ── Preview area — 20% smaller, centered in flexible space ──────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .fillMaxHeight(0.80f)
                            .onGloballyPositioned { coords ->
                                val root = rootCoordsRef[0] ?: return@onGloballyPositioned
                                val bounds = root.localBoundingBoxOf(coords, clipBounds = false)
                                glowCenter  = bounds.center
                                glowHalfDiag = sqrt(
                                    (bounds.width / 2f).pow(2f) + (bounds.height / 2f).pow(2f)
                                )
                            }
                            .clip(RoundedCornerShape(if (isAlt) 0.dp else 10.dp))
                            .border(
                                if (isAlt) 2.dp else 1.dp,
                                if (isAlt) accentColor else Color.White.copy(0.3f),
                                RoundedCornerShape(if (isAlt) 0.dp else 10.dp)
                            )
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
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )
                        if (gridEnabled) { GridOverlay() }
                        if (isSpider) { SpiderWebCorners() }
                        // Focus ring
                        focusPoint?.let { fp ->
                            FocusRing(offset = fp, onFinished = { focusPoint = null })
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── REC indicator — always in layout, invisible when idle ──────
                Box(Modifier.alpha(if (isRecording) 1f else 0f)) { RecIndicator() }
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
                                .then(when {
                                    isIceCream -> Modifier.background(Brush.linearGradient(listOf(icePink.copy(0.3f), icePeach.copy(0.3f))), CircleShape)
                                    isTropical -> Modifier.background(woodBrown.copy(0.3f), RoundedCornerShape(8.dp))
                                    isSpider   -> Modifier.background(Brush.linearGradient(listOf(spiderRed.copy(0.3f), spiderBlue.copy(0.3f))), CircleShape)
                                    isMatcha   -> Modifier.background(accentColor.copy(0.25f), RoundedCornerShape(8.dp))
                                    isAlt      -> Modifier
                                    else       -> Modifier.background(Color.White.copy(0.15f), CircleShape)
                                })
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
                                .then(when {
                                    isIceCream -> Modifier.background(
                                        Brush.linearGradient(listOf(icePink.copy(if (isScreenDimmed) 0.08f else 0.3f), icePeach.copy(if (isScreenDimmed) 0.08f else 0.3f))),
                                        CircleShape
                                    )
                                    isTropical -> Modifier.background(woodBrown.copy(if (isScreenDimmed) 0.1f else 0.3f), RoundedCornerShape(8.dp))
                                    isSpider   -> Modifier.background(
                                        Brush.linearGradient(listOf(spiderRed.copy(if (isScreenDimmed) 0.08f else 0.3f), spiderBlue.copy(if (isScreenDimmed) 0.08f else 0.3f))),
                                        CircleShape
                                    )
                                    isMatcha   -> Modifier.background(if (isScreenDimmed) accentColor.copy(0.08f) else accentColor.copy(0.25f), RoundedCornerShape(8.dp))
                                    isAlt      -> Modifier
                                    else       -> Modifier.background(if (isScreenDimmed) Color.White.copy(0.05f) else Color.White.copy(0.15f), CircleShape)
                                })
                                .clickable { viewModel.setScreenDimmed(!isScreenDimmed) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.WbSunny,
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
                                .then(when {
                                    isIceCream -> Modifier.background(Brush.linearGradient(listOf(icePink.copy(0.3f), icePeach.copy(0.3f))), CircleShape)
                                    isTropical -> Modifier.background(woodBrown.copy(0.3f), RoundedCornerShape(8.dp))
                                    isSpider   -> Modifier.background(Brush.linearGradient(listOf(spiderRed.copy(0.3f), spiderBlue.copy(0.3f))), CircleShape)
                                    isMatcha   -> Modifier.background(accentColor.copy(0.25f), RoundedCornerShape(8.dp))
                                    isAlt      -> Modifier
                                    else       -> Modifier.background(Color.White.copy(0.15f), CircleShape)
                                })
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
                                .then(when {
                                    isIceCream -> Modifier.background(Brush.linearGradient(listOf(icePink.copy(0.3f), icePeach.copy(0.3f))), CircleShape)
                                    isTropical -> Modifier.background(woodBrown.copy(0.3f), RoundedCornerShape(8.dp))
                                    isSpider   -> Modifier.background(Brush.linearGradient(listOf(spiderRed.copy(0.3f), spiderBlue.copy(0.3f))), CircleShape)
                                    isMatcha   -> Modifier.background(accentColor.copy(0.25f), RoundedCornerShape(8.dp))
                                    isAlt      -> Modifier
                                    else       -> Modifier.background(Color.White.copy(0.15f), CircleShape)
                                })
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

                Spacer(Modifier.height(24.dp))
            }

            // ── Full-screen dim overlay ───────────────────────────────────────
            if (isScreenDimmed) {
                DimOverlay(onWake = { viewModel.setScreenDimmed(false) })
            }
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
            .border(width = 3.dp, brush = gradientBrush, shape = CircleShape)
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
            .border(3.dp, accentColor, CircleShape)
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
private fun RecIndicator() {
    val alpha by rememberInfiniteTransition(label = "rec").animateFloat(
        initialValue = 1f,
        targetValue  = 0.15f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "recAlpha"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(RecordRed.copy(alpha = alpha)))
        Text(
            "REC",
            color = RecordRed.copy(alpha = alpha),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.W900,
            letterSpacing = 3.sp
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
            Icons.Default.WbSunny,
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
