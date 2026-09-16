package com.example.bodycamoutdoorgear.ui.transfer

import android.graphics.ImageFormat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scanned by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = Executors.newSingleThreadExecutor()
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analyzer.setAnalyzer(executor) { proxy ->
                        if (!scanned) {
                            val result = decodeQr(proxy)
                            if (result != null) {
                                scanned = true
                                onResult(result)
                            }
                        }
                        proxy.close()
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Dim overlay with transparent cutout + corner brackets
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
        ) {
            val boxSize = minOf(size.width, size.height) * 0.65f
            val left = (size.width - boxSize) / 2f
            val top = (size.height - boxSize) / 2f

            // Dark surround
            drawRect(Color.Black.copy(alpha = 0.62f))

            // Transparent cutout
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                blendMode = BlendMode.Clear
            )

            // Corner brackets
            val bracketLen = boxSize * 0.12f
            val strokeWidth = 4.dp.toPx()
            val corners = listOf(
                Offset(left, top) to listOf(Offset(left + bracketLen, top), Offset(left, top + bracketLen)),
                Offset(left + boxSize, top) to listOf(Offset(left + boxSize - bracketLen, top), Offset(left + boxSize, top + bracketLen)),
                Offset(left, top + boxSize) to listOf(Offset(left + bracketLen, top + boxSize), Offset(left, top + boxSize - bracketLen)),
                Offset(left + boxSize, top + boxSize) to listOf(Offset(left + boxSize - bracketLen, top + boxSize), Offset(left + boxSize, top + boxSize - bracketLen))
            )
            corners.forEach { (corner, ends) ->
                ends.forEach { end ->
                    drawLine(Color.White, corner, end, strokeWidth, StrokeCap.Round)
                }
            }
        }

        // Hint label below viewfinder
        Text(
            text = "Point your camera at the QR code",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 160.dp)
                .padding(horizontal = 32.dp)
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}

private fun decodeQr(proxy: ImageProxy): String? {
    return try {
        val plane = proxy.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val width = proxy.width
        val height = proxy.height

        val rowBytes = ByteArray(buffer.remaining())
        buffer.get(rowBytes)

        // Some devices pad each row to a stride wider than the image width
        // (hardware/ISP memory alignment). Reading the buffer as if it were
        // tightly packed skews the image and breaks decoding on those devices,
        // even though it happens to work on devices where rowStride == width.
        val data = if (rowStride == width) {
            rowBytes
        } else {
            ByteArray(width * height).also { packed ->
                for (row in 0 until height) {
                    System.arraycopy(rowBytes, row * rowStride, packed, row * width, width)
                }
            }
        }

        val source = PlanarYUVLuminanceSource(
            data, width, height,
            0, 0, width, height, false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE))
        MultiFormatReader().decode(bitmap, hints).text
    } catch (_: Exception) { null }
}
