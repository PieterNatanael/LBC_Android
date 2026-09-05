package com.example.bodycamoutdoorgear.ui.camera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.Brush
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
import com.example.bodycamoutdoorgear.viewmodel.CaptureStatus
import com.example.bodycamoutdoorgear.viewmodel.PhotoViewModel
import com.example.bodycamoutdoorgear.viewmodel.QualityTier
import com.example.bodycamoutdoorgear.viewmodel.SettingsViewModel
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoScreen(
    viewModel: PhotoViewModel,
    subscriptionManager: SubscriptionManager,
    onRequestPermissions: () -> Unit,
    onShowIntro: () -> Unit = {}
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val qualityTier      by settingsViewModel.photoQualityTier.collectAsState()
    val videoQualityTier by settingsViewModel.videoQualityTier.collectAsState()
    val gridEnabled      by settingsViewModel.gridEnabled.collectAsState()
    val isCapturing      by viewModel.isCapturing.collectAsState()
    val captureStatus    by viewModel.captureStatus.collectAsState()
    val isPreviewEnabled by viewModel.isPreviewEnabled.collectAsState()
    val isFrontCamera    by viewModel.isFrontCamera.collectAsState()
    val isScreenDimmed   by viewModel.isScreenDimmed.collectAsState()

    val camPerm      = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val permsGranted = camPerm

    val isSubscribed    by subscriptionManager.isSubscribed.collectAsState()
    val isGrandfathered by subscriptionManager.isGrandfathered.collectAsState()
    val isUnlocked = isSubscribed || isGrandfathered

    var showPaywall  by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView    = remember { PreviewView(context) }
    var imageCapture   by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl  by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var cameraInfo     by remember { mutableStateOf<androidx.camera.core.CameraInfo?>(null) }
    var focusPoint     by remember { mutableStateOf<Offset?>(null) }
    var zoomRatio      by remember { mutableStateOf(1f) }

    // Rebind camera when facing or quality changes
    LaunchedEffect(isFrontCamera, qualityTier, permsGranted) {
        if (!permsGranted) return@LaunchedEffect
        val cameraProvider = suspendCoroutine<ProcessCameraProvider> { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
        }
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val iCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(settingsViewModel.photoJpegQuality)
            .build()
        val selector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                       else CameraSelector.DEFAULT_BACK_CAMERA
        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, iCapture)
            imageCapture = iCapture
            cameraControl = camera.cameraControl
            cameraInfo = camera.cameraInfo
            zoomRatio = 1f
        } catch (e: Exception) {
            viewModel.setCaptureStatus(CaptureStatus.FAILURE)
        }
    }

    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

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

    fun takePhoto() {
        val ic = imageCapture ?: return
        viewModel.setCapturing(true)
        val photoFile = File(context.filesDir, "bodycam_${System.currentTimeMillis()}.jpg")
        val opts = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        ic.takePicture(opts, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModel.setCapturing(false)
                    viewModel.setCaptureStatus(CaptureStatus.SUCCESS)
                    MediaScannerConnection.scanFile(
                        context, arrayOf(photoFile.absolutePath), arrayOf("image/jpeg"), null
                    )
                }
                override fun onError(e: ImageCaptureException) {
                    viewModel.setCapturing(false)
                    viewModel.setCaptureStatus(CaptureStatus.FAILURE)
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
    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        if (!permsGranted) {
            PhotoPermissionRequest(onRequest = onRequestPermissions)
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
                    // PRO chip
                    if (!isUnlocked) {
                        SubtleProChip(onClick = { showPaywall = true })
                    }
                }

                // ── Preview area — flexible height, shrinks if needed ─────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(10.dp))
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
                    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                    if (gridEnabled) { GridOverlay() }
                    // Focus ring
                    focusPoint?.let { fp ->
                        FocusRing(offset = fp, onFinished = { focusPoint = null })
                    }

                    // White flash overlay on capture
                    if (isCapturing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.55f))
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Status badge ─────────────────────────────────────────────
                Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) {
                    when (captureStatus) {
                        CaptureStatus.SUCCESS -> PhotoStatusBadge("SAVED", SuccessGreen)
                        CaptureStatus.FAILURE -> PhotoStatusBadge("FAILED", RecordRed)
                        CaptureStatus.SAVING  -> PhotoStatusBadge("SAVING...", Color.White.copy(0.5f))
                        null                  -> {}
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ── Shutter + Flip/Dim left + Settings right ─────────────────
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    KnurledShutterButton(
                        isCapturing = isCapturing,
                        onClick = { if (!isCapturing) takePhoto() }
                    )
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
                                .background(Color.White.copy(0.15f), CircleShape)
                                .clickable(enabled = !isCapturing) { viewModel.flipCamera() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Cameraswitch,
                                contentDescription = "Flip camera",
                                tint = if (isCapturing) Color.White.copy(0.3f) else Color.White.copy(0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isScreenDimmed) Color.White.copy(0.05f) else Color.White.copy(0.15f),
                                    CircleShape
                                )
                                .clickable { viewModel.setScreenDimmed(!isScreenDimmed) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.WbSunny,
                                contentDescription = "Dim screen",
                                tint = if (isScreenDimmed) Color.White.copy(0.3f) else Color.White.copy(0.85f),
                                modifier = Modifier.size(22.dp)
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
                                .background(Color.White.copy(0.15f), CircleShape)
                                .clickable {
                                    zoomRatio = 1f
                                    cameraControl?.setZoomRatio(1f)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (zoomRatio == 1f) "1×" else "%.1f×".format(zoomRatio),
                                color = Color.White.copy(0.85f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable { showSettings = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // ── Full-screen dim overlay ───────────────────────────────────────
            if (isScreenDimmed) {
                PhotoDimOverlay(onWake = { viewModel.setScreenDimmed(false) })
            }
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
