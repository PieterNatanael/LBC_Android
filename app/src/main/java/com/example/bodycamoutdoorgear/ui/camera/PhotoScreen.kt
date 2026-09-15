package com.example.bodycamoutdoorgear.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.media.MediaScannerConnection
import android.media.ToneGenerator
import android.media.AudioManager
import android.view.WindowManager
import java.io.File
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.Brush
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
import com.example.bodycamoutdoorgear.manager.SubscriptionManager
import com.example.bodycamoutdoorgear.ui.paywall.PaywallScreen
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.BorderColor
import com.example.bodycamoutdoorgear.ui.theme.ButtonFace
import com.example.bodycamoutdoorgear.ui.theme.CompassGold
import com.example.bodycamoutdoorgear.ui.theme.RecordRed
import com.example.bodycamoutdoorgear.ui.theme.SuccessGreen
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary
import com.example.bodycamoutdoorgear.viewmodel.AppearanceMode
import com.example.bodycamoutdoorgear.viewmodel.CaptureStatus
import com.example.bodycamoutdoorgear.viewmodel.DisplayMode
import com.example.bodycamoutdoorgear.viewmodel.PhotoViewModel
import com.example.bodycamoutdoorgear.viewmodel.QualityTier
import com.example.bodycamoutdoorgear.viewmodel.SettingsViewModel
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.compose.ui.graphics.Paint
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoScreen(
    viewModel: PhotoViewModel,
    subscriptionManager: SubscriptionManager,
    onRequestPermissions: () -> Unit,
    onShowIntro: () -> Unit = {},
    isActive: Boolean = true
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val qualityTier      by settingsViewModel.photoQualityTier.collectAsState()
    val videoQualityTier by settingsViewModel.videoQualityTier.collectAsState()
    val gridEnabled      by settingsViewModel.gridEnabled.collectAsState()
    val stampTimestamp   by settingsViewModel.stampTimestamp.collectAsState()
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
    val matchaGreen  = Color(0xFF7BAF52)
    val icePink      = Color(0xFFFF8FAB)
    val icePeach     = Color(0xFFFFB347)
    val woodBrown    = Color(0xFF6B3A2A)
    val coconutCream = Color(0xFFA0522D)
    val spiderRed    = Color(0xFFBB0000)
    val spiderBlue   = Color(0xFF0033BB)
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
    val glowColor = if (isSimple) Color(0xFFFF2020) else accentColor
    val matchaBg      = Color(0xFF0D1208)   // dark base — glow provides the color
    val iceCreamBg    = Color(0xFF110609)
    val tropicalBg    = Color(0xFF0E0803)
    val spiderBg      = Color(0xFF05050A)
    val isCapturing      by viewModel.isCapturing.collectAsState()
    val captureStatus    by viewModel.captureStatus.collectAsState()
    val isPreviewEnabled by viewModel.isPreviewEnabled.collectAsState()
    val isFrontCamera    by viewModel.isFrontCamera.collectAsState()
    val isScreenDimmed   by viewModel.isScreenDimmed.collectAsState()

    fun checkPerms() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    var permsGranted by remember { mutableStateOf(checkPerms()) }

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
    var showFlash    by remember { mutableStateOf(false) }
    val scope        = rememberCoroutineScope()

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
    val cameraPreviewContent = remember {
        movableContentOf {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }
    }
    val providerRef     = remember { arrayOfNulls<ProcessCameraProvider>(1) }
    val previewRef      = remember { arrayOfNulls<Preview>(1) }
    var imageCapture    by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl   by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var cameraInfo      by remember { mutableStateOf<androidx.camera.core.CameraInfo?>(null) }
    var focusPoint      by remember { mutableStateOf<Offset?>(null) }
    var zoomRatio       by remember { mutableStateOf(1f) }
    var minZoomRatio    by remember { mutableStateOf(1f) }
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

    LaunchedEffect(targetRotation, imageCapture) {
        imageCapture?.targetRotation = targetRotation
    }

    // Rebind camera when facing, quality, permission, or active-tab changes
    LaunchedEffect(isFrontCamera, qualityTier, permsGranted, isActive) {
        if (!permsGranted || !isActive) {
            // Do NOT call unbindAll here — CameraScreen's active bind also calls unbindAll,
            // and a concurrent unbindAll from the inactive screen would undo that bind.
            imageCapture = null
            return@LaunchedEffect
        }
        imageCapture = null
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
        val iCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(settingsViewModel.photoJpegQuality)
            .build()
        val selector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                       else CameraSelector.DEFAULT_BACK_CAMERA
        try {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, iCapture)
            imageCapture = iCapture
            cameraControl = camera.cameraControl
            cameraInfo = camera.cameraInfo
            zoomRatio = 1f
        } catch (e: Exception) {
            viewModel.setCaptureStatus(CaptureStatus.FAILURE)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            providerRef[0]?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    // Observe real zoom from hardware — keeps display in sync with hardware max
    DisposableEffect(cameraInfo) {
        val info = cameraInfo ?: return@DisposableEffect onDispose {}
        val observer = androidx.lifecycle.Observer<androidx.camera.core.ZoomState> { state ->
            zoomRatio = state.zoomRatio
            minZoomRatio = state.minZoomRatio
        }
        info.zoomState.observeForever(observer)
        onDispose { info.zoomState.removeObserver(observer) }
    }

    // Screen brightness + preview pipeline
    LaunchedEffect(isScreenDimmed) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val params = activity.window.attributes
        params.screenBrightness =
            if (isScreenDimmed) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = params

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

    fun takePhoto() {
        val ic = imageCapture ?: return
        // Flash + tone then immediately re-enable the button — image saves in background
        scope.launch {
            viewModel.setCapturing(true)
            showFlash = true
            delay(60)
            showFlash = false
            viewModel.setCapturing(false)
        }
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 35).apply {
                startTone(ToneGenerator.TONE_PROP_BEEP, 60)
            }
        } catch (_: Exception) {}

        val photoFile = File(context.filesDir, "LBC_${System.currentTimeMillis()}.jpg")
        val opts = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        val shouldStamp = stampTimestamp
        ic.takePicture(opts, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModel.setCaptureStatus(null)
                    if (shouldStamp) {
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            bakeTimestampOnPhoto(photoFile)
                            MediaScannerConnection.scanFile(
                                context, arrayOf(photoFile.absolutePath), arrayOf("image/jpeg"), null
                            )
                        }
                    } else {
                        MediaScannerConnection.scanFile(
                            context, arrayOf(photoFile.absolutePath), arrayOf("image/jpeg"), null
                        )
                    }
                }
                override fun onError(e: ImageCaptureException) {
                    viewModel.setCaptureStatus(null)
                }
            }
        )
    }

    // Volume button hardware trigger
    LaunchedEffect(Unit) {
        viewModel.hardwareButtonEvent.collect {
            if (!isCapturing) takePhoto()
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
            PhotoPermissionRequest(onRequest = onRequestPermissions)
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
                                    val newZoom = (zoomRatio * zoom).coerceAtLeast(minZoomRatio)
                                    try { cameraControl?.setZoomRatio(newZoom) } catch (_: Exception) {}
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        cameraPreviewContent()
                        if (gridEnabled) { GridOverlay() }
                        focusPoint?.let { fp -> FocusRing(offset = fp, onFinished = { focusPoint = null }) }
                        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = if (showFlash) 0.85f else 0f)))
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
                                        isCircle      -> Modifier.widthIn(max = 420.dp).fillMaxWidth(0.82f).aspectRatio(1f)
                                        isSaveBattery -> Modifier.widthIn(max = 420.dp).heightIn(max = 560.dp).fillMaxWidth(0.72f).fillMaxHeight(0.80f)
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
                                            val newZoom = (zoomRatio * zoom).coerceAtLeast(minZoomRatio)
                                            try { cameraControl?.setZoomRatio(newZoom) } catch (_: Exception) {}
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                cameraPreviewContent()
                                if (gridEnabled) { GridOverlay() }
                                if (isSpider) { SpiderWebCorners() }
                                focusPoint?.let { fp -> FocusRing(offset = fp, onFinished = { focusPoint = null }) }
                                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = if (showFlash) 0.85f else 0f)))
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Spacer(Modifier.height(28.dp))
                    Spacer(Modifier.height(6.dp))

                // ── Shutter + Flip/Dim left + Settings right ─────────────────
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGradientBtn) {
                        GradientShutterButton(
                            isCapturing = isCapturing,
                            gradientBrush = when {
                                isTropical -> tropicalBrush
                                isSpider   -> spiderBrush
                                else       -> iceBrush
                            },
                            onClick = { if (!isCapturing) takePhoto() }
                        )
                    } else if (isAlt) {
                        BauhausShutterButton(
                            isCapturing = isCapturing,
                            accentColor = accentColor,
                            innerRatio = if (isMatcha) 0.75f else 0.6f,
                            onClick = { if (!isCapturing) takePhoto() }
                        )
                    } else {
                        KnurledShutterButton(
                            isCapturing = isCapturing,
                            onClick = { if (!isCapturing) takePhoto() }
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
                                .clickable(enabled = !isCapturing) { viewModel.flipCamera() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Cameraswitch,
                                contentDescription = "Flip camera",
                                tint = if (isCapturing) accentColor.copy(0.3f) else if (isAlt) accentColor else Color.White,
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
                    PhotoDimOverlay(onWake = { viewModel.setScreenDimmed(false) })
                }
            } // end content Box
        }
    }

    if (showPaywall) {
        PaywallScreen(
            subscriptionManager = subscriptionManager,
            onDismiss = { showPaywall = false }
        )
    }

    if (showSettings) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState,
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "SETTINGS",
                    color = Color.White.copy(0.4f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )

                // Appearance
                Text("APPEARANCE", color = Color.White.copy(0.4f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
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
                Text("VIDEO QUALITY", color = Color.White.copy(0.4f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QualityTier.entries.forEach { tier ->
                        val selected = tier == videoQualityTier
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color.White.copy(0.25f) else Color.White.copy(0.1f))
                                .border(1.dp, if (selected) Color.White.copy(0.5f) else Color.White.copy(0.2f), RoundedCornerShape(6.dp))
                                .clickable { settingsViewModel.setVideoQualityTier(tier) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tier.shortLabel, color = if (selected) Color.White else Color.White.copy(0.4f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                // Photo Quality
                Text("PHOTO QUALITY", color = Color.White.copy(0.4f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QualityTier.entries.forEach { tier ->
                        val selected = tier == qualityTier
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color.White.copy(0.25f) else Color.White.copy(0.1f))
                                .border(1.dp, if (selected) Color.White.copy(0.5f) else Color.White.copy(0.2f), RoundedCornerShape(6.dp))
                                .clickable { settingsViewModel.setPhotoQualityTier(tier) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tier.shortLabel, color = if (selected) Color.White else Color.White.copy(0.4f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                    Switch(
                        checked = gridEnabled,
                        onCheckedChange = { settingsViewModel.toggleGrid() }
                    )
                }

                // Bake timestamp into photo file
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PHOTO TIMESTAMP", color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Switch(
                        checked = stampTimestamp,
                        onCheckedChange = { settingsViewModel.toggleStampTimestamp() }
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
                    Switch(
                        checked = showGalleryTimestamp,
                        onCheckedChange = { settingsViewModel.toggleGalleryTimestamp() }
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

                // Also from us
                Text(
                    "Our Apps",
                    color = Color.White.copy(0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gogoodmood.com/apps/")))
                    }
                )

                // Privacy & Terms
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Privacy Policy",
                        color = Color.White.copy(0.4f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gogoodmood.com/apps/en/bodycamz/privacy/")))
                        }
                    )
                    Text("  ·  ", color = Color.White.copy(0.25f), fontSize = 11.sp)
                    Text(
                        "Terms of Use",
                        color = Color.White.copy(0.4f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gogoodmood.com/apps/en/bodycamz/terms/")))
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Knurled Shutter Button ────────────────────────────────────────────────────

@Composable
fun KnurledShutterButton(isCapturing: Boolean, onClick: () -> Unit, size: Dp = 100.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clickable(enabled = !isCapturing, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Tick marks
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
                        if (isCapturing) Brush.radialGradient(
                            colors = listOf(Color.White.copy(0.5f), Color.White.copy(0.3f)),
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
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Take photo",
                    tint = if (isCapturing) Color.White.copy(0.4f) else Color(0xFF1A1A1A),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun GradientShutterButton(isCapturing: Boolean, onClick: () -> Unit, gradientBrush: Brush, size: Dp = 70.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(width = 3.dp, color = Color.White, shape = CircleShape)
            .background(Color(0xFF100608))
            .clickable(enabled = !isCapturing, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.75f)
                .clip(CircleShape)
                .background(gradientBrush.let { if (isCapturing) Brush.linearGradient(listOf(Color.White.copy(0.2f), Color.White.copy(0.1f))) else it }),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = "Take photo",
                tint = if (isCapturing) Color.Black.copy(0.3f) else Color.Black,
                modifier = Modifier.size(size * 0.35f)
            )
        }
    }
}

@Composable
fun BauhausShutterButton(isCapturing: Boolean, onClick: () -> Unit, accentColor: Color = Color.White, innerRatio: Float = 0.6f, size: Dp = 70.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(3.dp, Color.White, CircleShape)
            .background(Color.Black)
            .clickable(enabled = !isCapturing, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * innerRatio)
                .clip(CircleShape)
                .background(if (isCapturing) accentColor.copy(0.4f) else accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = "Take photo",
                tint = if (isCapturing) Color.Black.copy(0.3f) else Color.Black,
                modifier = Modifier.size(size * 0.32f)
            )
        }
    }
}

// ── Supporting composables ────────────────────────────────────────────────────

@Composable
private fun PhotoStatusBadge(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(
            text,
            color = color,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.W900,
            letterSpacing = 3.sp
        )
    }
}

@Composable
private fun PhotoPreviewToggleButton(isOn: Boolean, onClick: () -> Unit) {
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
            imageVector = Icons.Default.CameraAlt,
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
private fun PhotoDimScreenButton(isDimmed: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
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
                "saves battery",
                color = if (isDimmed) Color.White.copy(0.25f) else Color.White.copy(0.38f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun PhotoDimOverlay(onWake: () -> Unit) {
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
private fun PhotoPermissionRequest(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Camera Access Required", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Camera permission is needed to take photos.", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = ButtonFace)
            ) { Text("Grant Permission", letterSpacing = 1.sp) }
        }
    }
}

private fun bakeTimestampOnPhoto(file: java.io.File) {
    // Read EXIF rotation so we can produce an upright image before stamping
    val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
    val orientation = exif.getAttributeInt(
        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
    )
    val rotateDeg = when (orientation) {
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90  -> 90f
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    val raw = android.graphics.BitmapFactory.decodeFile(file.absolutePath) ?: return

    // Physically rotate to upright, then copy to a mutable bitmap for Canvas
    val upright = if (rotateDeg != 0f) {
        val m = android.graphics.Matrix().apply { postRotate(rotateDeg) }
        android.graphics.Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
            .also { raw.recycle() }
    } else {
        raw
    }
    val bitmap = upright.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
    upright.recycle()

    val canvas = android.graphics.Canvas(bitmap)
    val label = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date()) + " · LBC"
    val textSize = (bitmap.width * 0.032f).coerceIn(28f, 96f)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        this.textSize = textSize
        typeface = android.graphics.Typeface.MONOSPACE
        setShadowLayer(textSize * 0.25f, 4f, 4f, android.graphics.Color.parseColor("#99000000"))
        textAlign = android.graphics.Paint.Align.RIGHT
    }
    val margin = textSize * 1.4f
    canvas.drawText(label, bitmap.width - margin, bitmap.height - margin, paint)

    java.io.FileOutputStream(file).use { out ->
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
    }
    bitmap.recycle()

    // The image is now physically upright; reset EXIF orientation so viewers don't re-rotate it
    val outExif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
    outExif.setAttribute(
        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL.toString()
    )
    outExif.saveAttributes()
}
