package com.example.bodycamoutdoorgear.ui.transfer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.bodycamoutdoorgear.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bodycamoutdoorgear.transfer.TRANSFER_PORT
import com.example.bodycamoutdoorgear.transfer.TransferClient
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.RecordRed
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ReceiveScreen(onDismiss: () -> Unit, onFilesReceived: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hostIp by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Scan the QR code on the sender's device") }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val receivedFiles = remember { mutableStateListOf<String>() }

    var showScanner by remember { mutableStateOf(false) }
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showScanner = true
    }

    fun onQrScanned(raw: String) {
        showScanner = false
        val uri = android.net.Uri.parse(raw)
        val ip = uri.getQueryParameter("host") ?: return
        hostIp = ip
        status = "Connected to $ip. Ready to receive."
        scope.launch {
            isDownloading = true
            status = "Fetching file list..."
            val files = TransferClient.listFiles(ip, TRANSFER_PORT)
            if (files.isEmpty()) {
                status = "No files found. Error: ${TransferClient.lastError.ifEmpty { "empty list" }}"
                isDownloading = false
                return@launch
            }
            status = "Downloading ${files.size} file(s)..."
            var done = 0
            files.forEach { name ->
                val ok = TransferClient.downloadFile(context, ip, TRANSFER_PORT, name) { p ->
                    progress = (done + p) / files.size.toFloat()
                }
                if (ok) { done++; receivedFiles.add(name) }
            }
            progress = 1f
            isDownloading = false
            status = "Done. $done file(s) saved to gallery."
            if (done > 0) onFilesReceived()
        }
    }

    if (showScanner) {
        QrScannerScreen(
            onResult = ::onQrScanned,
            onDismiss = { showScanner = false }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "HipHop",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Image(
                painter = painterResource(id = R.drawable.hiphop),
                contentDescription = "HipHop",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Text(
                "Both devices must be on the same WiFi.\nOpen LBC on the other device, tap HipHop,\nthen scan the QR code shown there.",
                color = Color.White.copy(0.4f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Text(
                status,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White
                )
            }

            if (receivedFiles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    receivedFiles.forEach { name ->
                        Text(name, color = Color.White.copy(0.6f), fontSize = 12.sp)
                    }
                }
            }

            if (hostIp == null && !isDownloading) {
                Button(
                    onClick = {
                        if (hasCameraPermission) showScanner = true
                        else permLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan QR Code", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
