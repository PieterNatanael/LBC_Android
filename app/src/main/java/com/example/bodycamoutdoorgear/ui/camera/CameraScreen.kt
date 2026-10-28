package com.example.bodycamoutdoorgear.ui.camera

import com.example.bodycamoutdoorgear.i18n.Tr
import com.example.bodycamoutdoorgear.i18n.AppLanguage
import com.example.bodycamoutdoorgear.i18n.trf
import com.example.bodycamoutdoorgear.i18n.tr
import android.Manifest
import com.example.bodycamoutdoorgear.ui.theme.themedShape
import com.example.bodycamoutdoorgear.ui.theme.sideButtonStyle
import com.example.bodycamoutdoorgear.ui.theme.chromeAccentOr
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.WindowManager
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.effects.OverlayEffect
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalClipboardManager
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
import com.example.bodycamoutdoorgear.ui.settings.PrivateGallerySettingsSection
import com.example.bodycamoutdoorgear.ui.settings.AppUpdateSection
import com.example.bodycamoutdoorgear.ui.settings.TurnOffLightSection
import com.example.bodycamoutdoorgear.ui.settings.MoreButtonsSection
import com.example.bodycamoutdoorgear.ui.settings.ShutterSoundSettings
import com.example.bodycamoutdoorgear.manager.SubscriptionManager
import com.example.bodycamoutdoorgear.ui.paywall.PaywallScreen
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.MarsRecordButton
import com.example.bodycamoutdoorgear.ui.theme.BorderColor
import com.example.bodycamoutdoorgear.ui.theme.ButtonFace
import com.example.bodycamoutdoorgear.ui.theme.CompassGold
import com.example.bodycamoutdoorgear.ui.theme.RecordRed
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary
import com.example.bodycamoutdoorgear.viewmodel.CameraViewModel
import kotlinx.coroutines.delay
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
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    subscriptionManager: SubscriptionManager,
    onRequestPermissions: () -> Unit,
    onShowIntro: () -> Unit = {},
    isActive: Boolean = true,
    onOpenGallery: () -> Unit = {}
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val isRecording      by viewModel.isRecording.collectAsState()
    val qualityTier      by settingsViewModel.videoQualityTier.collectAsState()
    val photoQualityTier by settingsViewModel.photoQualityTier.collectAsState()
    val gridEnabled      by settingsViewModel.gridEnabled.collectAsState()
    val stampTimestamp   by settingsViewModel.stampTimestamp.collectAsState()
    val appearanceMode   by settingsViewModel.appearanceMode.collectAsState()
    val displayMode      by settingsViewModel.displayMode.collectAsState()
    val isSaveBattery    = displayMode == DisplayMode.SAVE_BATTERY
    val isCircle         = displayMode == DisplayMode.CIRCLE
    val isYapping        = displayMode == DisplayMode.YAPPING
    val isGiveaway       = displayMode.makesCard   // Giveaway or For Sale: the clip becomes a card
    // The recording waiting to be made into a Giveaway card, while its sheet is open.
    var giveawayVideoPath by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val isSimple   = appearanceMode == AppearanceMode.SIMPLE
    val isTactical = appearanceMode == AppearanceMode.TACTICAL
    val isMatcha   = appearanceMode == AppearanceMode.MATCHA
    val isIceCream = appearanceMode == AppearanceMode.ICE_CREAM
    val isTropical = appearanceMode == AppearanceMode.TROPICAL
    val isSpider   = appearanceMode == AppearanceMode.SPIDER
    val isMars     = appearanceMode == AppearanceMode.MARS
    val isGradientBtn = isIceCream || isTropical || isSpider
    val isAlt = isSimple || isTactical || isMatcha || isIceCream || isTropical || isSpider || isMars
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
        AppearanceMode.MARS      -> com.example.bodycamoutdoorgear.ui.theme.MarsRust
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
    var showAiMenu by remember { mutableStateOf(false) }

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
    // Flash ring button: 0 = off, 1 = on (continuous torch).
    var flashMode       by remember { mutableStateOf(0) }
    var cameraInfo      by remember { mutableStateOf<androidx.camera.core.CameraInfo?>(null) }
    var focusPoint      by remember { mutableStateOf<Offset?>(null) }
    var zoomRatio       by remember { mutableStateOf(1f) }
    var minZoomRatio    by remember { mutableStateOf(1f) }
    var maxZoomRatio    by remember { mutableStateOf(1f) }
    val zoomGlider = rememberZoomGlider { ratio ->
        try { cameraControl?.setZoomRatio(ratio) } catch (_: Exception) {}
    }
    val initialRotation = remember { (context as? Activity)?.display?.rotation ?: android.view.Surface.ROTATION_0 }
    var targetRotation  by remember { mutableStateOf(initialRotation) }

    // ── Video timestamp overlay ─────────────────────────────────────────────────
    // Rebuilt from scratch (see project memory for the history of failed attempts
    // at orientation-correcting the text — all of them introduced a duplicate
    // stamp). This version does the minimum possible: no orientation branching, no
    // transform, just lock onto whichever exact canvas size CameraX hands us first
    // for a given recording and refuse every other size for the rest of it. That's
    // a structural guarantee against duplicates regardless of *why* CameraX is
    // invoking the listener more than once (undocumented, and not worth guessing at
    // again) — it can only ever draw on one specific buffer per recording.
    val videoStampTextRef       = remember { java.util.concurrent.atomic.AtomicReference<String?>(null) }
    val videoStampLockedSizeRef = remember { java.util.concurrent.atomic.AtomicReference<Pair<Int, Int>?>(null) }

    val videoStampEffect = remember {
        OverlayEffect(
            CameraEffect.VIDEO_CAPTURE,
            0,
            android.os.Handler(android.os.Looper.getMainLooper()),
            {}
        ).also { effect ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                typeface = StampStyle.typeface(context)
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            effect.setOnDrawListener { frame ->
                val text = videoStampTextRef.get() ?: return@setOnDrawListener true  // inactive
                val canvas = frame.overlayCanvas
                val size = canvas.width to canvas.height
                val lockedSize = videoStampLockedSizeRef.updateAndGet { it ?: size }
                if (lockedSize != size) return@setOnDrawListener true  // not the locked buffer — skip

                val bw = canvas.width.toFloat()
                val bh = canvas.height.toFloat()
                val textSize = (minOf(bw, bh) * 0.026f).coerceIn(12f, 34f)
                paint.textSize = textSize
                paint.setShadowLayer(textSize * 0.25f, 3f, 3f, 0x99000000.toInt())
                val margin = textSize * 0.6f
                // One line if it fits the frame, otherwise date over tagline,
                // so a narrow (low quality) frame never clips the stamp.
                val lines = StampStyle.lines(paint, bw - margin * 2, text)
                StampStyle.draw(canvas, paint, lines, bw - margin, bh - margin)
                true
            }
        }
    }

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
    // Torch belongs to one camera and one binding: re-apply after every
    // rebind, and drop it on flip (front has none) or when the tab is left.
    LaunchedEffect(flashMode, cameraControl) {
        try { cameraControl?.enableTorch(flashMode != 0) } catch (_: Exception) {}
    }
    LaunchedEffect(isFrontCamera, isActive) { flashMode = 0 }

    LaunchedEffect(qualityTier, isFrontCamera, permsGranted, isActive) {
        zoomGlider.cancel()  // tab left, camera flipped or rebound: drop any running glide
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
            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(vCapture)
                .addEffect(videoStampEffect)
                .build()
            val camera = provider.bindToLifecycle(lifecycleOwner, selector, useCaseGroup)
            videoCapture = vCapture
            cameraControl = camera.cameraControl
            cameraInfo = camera.cameraInfo
            zoomRatio = 1f
        } catch (e: Exception) {
            viewModel.setError(trf("Camera error: %s", e.message ?: ""))
        }
    }

    // Stop and save recording when user navigates to a different tab
    LaunchedEffect(isActive) {
        if (!isActive && viewModel.isRecording.value) {
            viewModel.activeRecording?.stop()
            viewModel.activeRecording = null
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
            minZoomRatio = state.minZoomRatio
            maxZoomRatio = state.maxZoomRatio
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
            viewModel.setError(tr("Not enough storage. Free up space and try again."))
            return
        }
        val giveawayShot = isGiveaway
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
                            if (giveawayShot) {
                                // Giveaway: the clip waits in a temporary folder until it is made into a card.
                                val clip = File(com.example.bodycamoutdoorgear.giveaway.GiveawayStore.originalsDir(context), "LBC_${System.currentTimeMillis()}.mp4")
                                if (tmp.renameTo(clip) || runCatching { tmp.copyTo(clip, overwrite = true); tmp.delete() }.isSuccess) {
                                    giveawayVideoPath = clip.absolutePath
                                }
                            } else {
                                tmp.renameTo(File(context.filesDir, "LBC_${System.currentTimeMillis()}.mp4"))
                            }
                        } else {
                            tmp.delete()
                            viewModel.setError(trf("Recording failed (error %d)", event.error))
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

    // Giveaway cards hold a short clip, so recording stops by itself.
    LaunchedEffect(isRecording, isGiveaway) {
        if (isRecording && isGiveaway) {
            delay(com.example.bodycamoutdoorgear.giveaway.GiveawayVideoExporter.MAX_SECONDS * 1000L)
            stopRecording()
        }
    }

    // Volume button hardware trigger
    LaunchedEffect(Unit) {
        viewModel.hardwareButtonEvent.collect {
            if (viewModel.isRecording.value) stopRecording() else startRecording()
        }
    }

    // Drives the video overlay while recording, using the same TIMESTAMP toggle as photos.
    // Not in Giveaway mode: the card prints the date itself.
    LaunchedEffect(isRecording, stampTimestamp, isGiveaway) {
        if (isRecording && stampTimestamp && !isGiveaway) {
            videoStampLockedSizeRef.set(null)  // fresh recording — re-lock to its first buffer
            while (true) {
                videoStampTextRef.set(StampStyle.dateText())
                delay(1000)
            }
        } else {
            videoStampTextRef.set(null)
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

        // Glow + vignette — skipped in YAPPING, whose preview doesn't update
        // previewBounds (it floats independently), so the glow would otherwise
        // render around a stale rectangle from whatever mode was active before
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pB = previewBounds
            if (pB.width > 0f && !isYapping) {
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
                val isNormalDisplay = !isCircle && !isSaveBattery && !isYapping
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
                                    if (zoom != 1f) zoomGlider.cancel()
                                    val newZoom = (zoomRatio * zoom).coerceAtLeast(minZoomRatio)
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

                    // Preview slot — CIRCLE/SAVE_BATTERY show the preview here; YAPPING
                    // shows the script instead, with the preview floating separately
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isYapping) {
                            val yappingScript by settingsViewModel.yappingScript.collectAsState()
                            val clipboardManager = LocalClipboardManager.current
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = yappingScript,
                                    onValueChange = { settingsViewModel.setYappingScript(it) },
                                    placeholder = {
                                        Text(
                                            tr("Type or paste your script here…"),
                                            color = Color.White.copy(0.35f),
                                            fontSize = 18.sp
                                        )
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        lineHeight = 27.sp
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = accentColor.copy(0.6f),
                                        unfocusedBorderColor = Color.White.copy(0.15f),
                                        cursorColor = accentColor
                                    ),
                                    // Always narrow — a shorter line length means less
                                    // eye travel per line while reading during a take.
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(0.62f)
                                        .align(Alignment.CenterHorizontally)
                                )
                                Row(
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        tr("PASTE"),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .background(Color.Black, themedShape(4.dp))
                                            .clickable {
                                                clipboardManager.getText()?.text?.let {
                                                    settingsViewModel.setYappingScript(it)
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                    Text(
                                        tr("CLEAR"),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .background(Color.Black, themedShape(4.dp))
                                            .clickable { settingsViewModel.setYappingScript("") }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        } else if (!isNormalDisplay) {
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
                                            if (zoom != 1f) zoomGlider.cancel()
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
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Spacer(Modifier.height(6.dp))
                    Spacer(Modifier.height(10.dp))

                // ── Record button + Flip/Dim left + Settings right ────────────
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isMars) {
                        MarsRecordButton(
                            isRecording = isRecording,
                            onClick = { if (isRecording) stopRecording() else startRecording() }
                        )
                    } else if (isGradientBtn) {
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
                    // Left: revolver of flip / dim / flash — two visible, swipe to rotate
                    ButtonRing(
                        count = 4,
                        accentColor = accentColor,
                        isAlt = isAlt,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            // Keep the ring (and its chevron tap area) clear of the
                            // centred shutter on narrow screens, where 32dp of
                            // padding would let the chevron overlap it.
                            .padding(start = (LocalConfiguration.current.screenWidthDp / 2 - 41 - 124).coerceIn(8, 32).dp),
                    ) { index, ringEnabled ->
                        when (index) {
                            0 -> Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .sideButtonStyle(SurfaceDark, accentColor)
                                    .clickable(enabled = ringEnabled && !isRecording) { viewModel.flipCamera() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Cameraswitch,
                                    contentDescription = tr("Flip camera"),
                                    tint = if (isRecording) accentColor.copy(0.3f) else if (isAlt) accentColor else Color.White,
                                    modifier = Modifier.size(if (isAlt) 26.dp else 22.dp)
                                )
                            }
                            1 -> Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .sideButtonStyle(if (isScreenDimmed) SurfaceDark.copy(alpha = 0.5f) else SurfaceDark, accentColor)
                                    .clickable(enabled = ringEnabled) { viewModel.setScreenDimmed(!isScreenDimmed) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DarkMode,
                                    contentDescription = tr("Dim screen"),
                                    tint = if (isScreenDimmed) accentColor.copy(0.3f) else if (isAlt) accentColor else Color.White,
                                    modifier = Modifier.size(if (isAlt) 26.dp else 22.dp)
                                )
                            }
                            3 -> ZoomRingButton(
                                text = formatZoom(zoomRatio),
                                accentColor = accentColor,
                                isAlt = isAlt,
                                enabled = ringEnabled,
                                onClick = { zoomGlider.onChipTap(zoomRatio, minZoomRatio, maxZoomRatio) },
                            )
                            else -> FlashRingButton(
                                mode = flashMode,
                                allowsAuto = false,
                                accentColor = accentColor,
                                isAlt = isAlt,
                                enabled = ringEnabled,
                                onModeChange = { flashMode = it },
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
                        AICardButton(mode = displayMode, accentColor = accentColor, isAlt = isAlt) { showAiMenu = true }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .sideButtonStyle(SurfaceDark, accentColor)
                                .clickable { showSettings = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = tr("Settings"),
                                tint = if (isAlt) accentColor else Color.White,
                                modifier = Modifier.size(if (isAlt) 26.dp else 22.dp)
                            )
                        }
                    }
                }

                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    Spacer(Modifier.height((LocalConfiguration.current.screenHeightDp * 0.16f).dp.coerceAtLeast(100.dp)))
                } // end Column layout

                // The card mode that is on, shown at the top so it is never a surprise.
                if (displayMode.makesCard && !isRecording) {
                    AICardChip(
                        mode = displayMode,
                        onClick = { showAiMenu = true },
                        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp),
                    )
                }

                // ── Recording timer badge — top center, clears camera cutout ──
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 12.dp)
                            .align(Alignment.TopCenter),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        RecTimerBadge(isRecording)
                    }
                }

                // ── Floating movable preview (YAPPING mode) ───────────────────────
                if (isYapping) {
                    var yapOffset by remember { mutableStateOf(Offset.Zero) }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 56.dp, end = 16.dp)
                            .offset { IntOffset(yapOffset.x.roundToInt(), yapOffset.y.roundToInt()) }
                            .size(width = 110.dp, height = 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, accentColor, RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    yapOffset += dragAmount
                                }
                            }
                    ) {
                        cameraPreviewContent()
                    }
                }

                // ── Full-screen dim overlay ───────────────────────────────────────
                if (isScreenDimmed) {
                    DimOverlay(onWake = { viewModel.setScreenDimmed(false) })
                }
            } // end content Box
        }
    }

    // Error dialog
    giveawayVideoPath?.let { path ->
        com.example.bodycamoutdoorgear.giveaway.GiveawayCardSheet(
            photoFile = File(path),
            mode = displayMode.cardMode ?: com.example.bodycamoutdoorgear.giveaway.CardMode.GIVEAWAY,
            onClose = { giveawayVideoPath = null },
            onOpenGallery = onOpenGallery,
            isPremium = subscriptionManager.isUnlocked,
            isVideo = true,
        )
    }

    if (recordingError != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text(tr("Recording Error"), color = TextPrimary) },
            text  = { Text(recordingError ?: "", color = TextSecondary) },
            confirmButton = { Button(onClick = viewModel::clearError) { Text(tr("OK")) } },
            containerColor = SurfaceDark
        )
    }

    if (showPaywall) {
        PaywallScreen(
            subscriptionManager = subscriptionManager,
            onDismiss = { showPaywall = false }
        )
    }

    if (showAiMenu) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AICardMenu(
            current = displayMode,
            onPick = { picked ->
                settingsViewModel.setDisplayMode(CardModeSwitch.toggled(context, displayMode, picked))
                showAiMenu = false
            },
            onDismiss = { showAiMenu = false },
        )
    }

    // Settings bottom sheet
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceDark
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
            // Pinned header — stays visible while the settings below scroll, so the
            // close button is always reachable without scrolling back to the top.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    tr("SETTINGS"),
                    color = Color.White.copy(0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                IconButton(onClick = { showSettings = false }) {
                    Icon(Icons.Default.Close, contentDescription = tr("Close settings"), tint = Color.White.copy(0.7f))
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Appearance
                Text(
                    tr("APPEARANCE"),
                    color = chromeAccentOr(Color.White.copy(0.4f)),
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
                                        .clip(themedShape(6.dp))
                                        .background(if (selected) chromeAccentOr(Color.White).copy(0.25f) else Color.White.copy(0.1f))
                                        .border(1.dp, if (selected) chromeAccentOr(Color.White).copy(0.5f) else Color.White.copy(0.2f), themedShape(6.dp))
                                        .clickable { settingsViewModel.setAppearanceMode(mode) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(tr(mode.label), color = if (selected) chromeAccentOr(Color.White) else Color.White.copy(0.4f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }
                }

                // Language: follows the phone unless one is picked here
                Text(
                    tr("LANGUAGE"),
                    color = chromeAccentOr(Color.White.copy(0.4f)),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.chunked(3).forEach { rowLanguages ->
                        Row(modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowLanguages.forEach { language ->
                                val languageSelected = language == Tr.selected
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(themedShape(6.dp))
                                        .background(if (languageSelected) chromeAccentOr(Color.White).copy(0.25f) else Color.White.copy(0.1f))
                                        .border(1.dp, if (languageSelected) chromeAccentOr(Color.White).copy(0.5f) else Color.White.copy(0.2f), themedShape(6.dp))
                                        .clickable { Tr.select(language) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        // Each language is written in its own script, never translated.
                                        if (language == AppLanguage.SYSTEM) tr("System") else language.nativeName,
                                        color = if (languageSelected) chromeAccentOr(Color.White) else Color.White.copy(0.4f),
                                        fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }

                // Display Mode
                Text(
                    tr("DISPLAY MODE"),
                    color = chromeAccentOr(Color.White.copy(0.4f)),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                // Every mode is a full-width tile with a line saying what it does. Giveaway is first.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DisplayMode.pickerOrder.forEach { mode ->
                        val selected = mode == displayMode
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(themedShape(6.dp))
                                .background(if (selected) chromeAccentOr(Color.White).copy(0.25f) else Color.White.copy(0.1f))
                                .border(1.dp, if (selected) chromeAccentOr(Color.White).copy(0.5f) else Color.White.copy(0.2f), themedShape(6.dp))
                                .clickable { settingsViewModel.setDisplayMode(mode) }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(tr(mode.title), color = if (selected) chromeAccentOr(Color.White) else Color.White.copy(0.5f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(tr(mode.subtitle), color = Color.White.copy(0.4f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }

                // Video Quality
                Text(
                    tr("VIDEO QUALITY"),
                    color = chromeAccentOr(Color.White.copy(0.4f)),
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
                                .clip(themedShape(6.dp))
                                .background(if (selected) chromeAccentOr(Color.White).copy(0.25f) else Color.White.copy(0.1f))
                                .border(1.dp, if (selected) chromeAccentOr(Color.White).copy(0.5f) else Color.White.copy(0.2f), themedShape(6.dp))
                                .clickable { settingsViewModel.setVideoQualityTier(tier) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tr(tier.shortLabel), color = if (selected) chromeAccentOr(Color.White) else Color.White.copy(0.4f), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                // Photo Quality
                Text(
                    tr("PHOTO QUALITY"),
                    color = chromeAccentOr(Color.White.copy(0.4f)),
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
                                .clip(themedShape(6.dp))
                                .background(if (selected) chromeAccentOr(Color.White).copy(0.25f) else Color.White.copy(0.1f))
                                .border(1.dp, if (selected) chromeAccentOr(Color.White).copy(0.5f) else Color.White.copy(0.2f), themedShape(6.dp))
                                .clickable { settingsViewModel.setPhotoQualityTier(tier) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tr(tier.shortLabel), color = if (selected) chromeAccentOr(Color.White) else Color.White.copy(0.4f), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                ShutterSoundSettings(settingsViewModel, accentColor)

                // Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("GRID"), color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    androidx.compose.material3.Switch(
                        checked = gridEnabled,
                        onCheckedChange = { settingsViewModel.toggleGrid() }
                    )
                }

                // Timestamp overlay — shared toggle for both photo and video
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("TIMESTAMP"), color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    androidx.compose.material3.Switch(
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
                    Text(tr("GALLERY DATE"), color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    androidx.compose.material3.Switch(
                        checked = showGalleryTimestamp,
                        onCheckedChange = { settingsViewModel.toggleGalleryTimestamp() }
                    )
                }

                // Gallery columns
                Text(
                    tr("GALLERY COLUMNS"),
                    color = chromeAccentOr(Color.White.copy(0.4f)),
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
                                .clip(themedShape(6.dp))
                                .background(if (selected) chromeAccentOr(Color.White).copy(0.25f) else Color.White.copy(0.1f))
                                .border(1.dp, if (selected) chromeAccentOr(Color.White).copy(0.5f) else Color.White.copy(0.2f), themedShape(6.dp))
                                .clickable { settingsViewModel.setGalleryColumns(cols) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$cols", color = if (selected) chromeAccentOr(Color.White) else Color.White.copy(0.4f), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                TurnOffLightSection()

                MoreButtonsSection()

                PrivateGallerySettingsSection()

                AppUpdateSection()

                // View Intro Again
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(themedShape(6.dp))
                        .background(Color.White.copy(0.08f))
                        .border(1.dp, Color.White.copy(0.15f), themedShape(6.dp))
                        .clickable {
                            settingsViewModel.resetIntro()
                            showSettings = false
                            onShowIntro()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tr("VIEW INTRO AGAIN"),
                        color = Color.White.copy(0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                // Also from us
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(themedShape(8.dp))
                        .background(accentColor.copy(0.16f))
                        .border(1.dp, accentColor.copy(0.45f), themedShape(8.dp))
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gogoodmood.com/apps/")))
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tr("Check Out Our Other Apps"),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            tr("More Tools by LBC"),
                            color = Color.White.copy(0.55f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Privacy & Terms
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr("Privacy Policy"),
                        color = Color.White.copy(0.4f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(com.example.bodycamoutdoorgear.i18n.Tr.privacyUrl)))
                        }
                    )
                    Text("  ·  ", color = Color.White.copy(0.25f), fontSize = 11.sp)
                    Text(
                        tr("Terms of Use"),
                        color = Color.White.copy(0.4f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(com.example.bodycamoutdoorgear.i18n.Tr.termsUrl)))
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
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
                            .clip(themedShape(4.dp))
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
            .clip(themedShape(6.dp))
            .background(if (isOn) Color.White.copy(0.18f) else Color.White.copy(0.1f))
            .border(
                1.dp,
                if (isOn) Color.White.copy(0.45f) else Color.White.copy(0.2f),
                themedShape(6.dp)
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
            text = if (isOn) tr("PREVIEW ON") else tr("PREVIEW OFF"),
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
            .clip(themedShape(6.dp))
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
            .clip(themedShape(6.dp))
            .background(if (isDimmed) Color.White.copy(0.04f) else Color.White.copy(0.12f))
            .border(
                1.dp,
                if (isDimmed) Color.White.copy(0.15f) else Color.White.copy(0.3f),
                themedShape(6.dp)
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
                if (isDimmed) tr("SCREEN DIMMED") else tr("DIM SCREEN"),
                color = if (isDimmed) Color.White.copy(0.3f) else Color.White.copy(0.7f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                tr("saves battery for long recordings"),
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
            .clip(themedShape(5.dp))
            .border(1.dp, CompassGold.copy(alpha = 0.4f), themedShape(5.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = tr("Upgrade to Pro"),
            tint = CompassGold.copy(alpha = 0.7f),
            modifier = Modifier.size(10.dp)
        )
        Text(
            tr("PRO"),
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
            Text(tr("Camera & Microphone Access Required"), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(tr("Low Battery Camera needs camera and microphone permissions to record."), color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = ButtonFace)
            ) { Text(tr("Grant Permissions"), letterSpacing = 1.sp) }
        }
    }
}
