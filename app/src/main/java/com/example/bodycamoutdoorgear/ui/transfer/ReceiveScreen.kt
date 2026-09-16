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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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

    var showNetworkTip by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }
    var manualIp by remember { mutableStateOf("") }
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showScanner = true
    }

    fun connectToIp(ip: String) {
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
                var lastReported = -1f
                val ok = TransferClient.downloadFile(context, ip, TRANSFER_PORT, name) { p ->
                    val overall = (done + p) / files.size.toFloat()
                    // Chunk callbacks fire every 256 KB, which for large files can be
                    // hundreds of times per second — only push a recomposition when the
                    // visible progress actually moves, instead of on every chunk.
                    if (overall - lastReported >= 0.01f) {
                        lastReported = overall
                        progress = overall
                    }
                }
                if (ok) { done++; receivedFiles.add(name) }
            }
            progress = 1f
            isDownloading = false
            status = "Done. $done file(s) saved to gallery."
            if (done > 0) onFilesReceived()
        }
    }

    fun onQrScanned(raw: String) {
        showScanner = false
        val uri = android.net.Uri.parse(raw)
        val ip = uri.getQueryParameter("host") ?: return
        connectToIp(ip)
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
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.hiphop),
                contentDescription = "HipHop",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Text(
                "HipHop",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "HipHop transfers files directly between your devices. No internet needed, just WiFi.\nMake sure both devices are on the same WiFi network.\nOn the sender's device, open LBC and tap HipHop to show their code, then scan it here.",
                color = Color.White.copy(0.4f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(Color.White.copy(0.12f), CircleShape)
                    .clickable { showNetworkTip = true },
                contentAlignment = Alignment.Center
            ) {
                Text("?", color = Color.White.copy(0.55f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            if (showNetworkTip) {
                AlertDialog(
                    onDismissRequest = { showNetworkTip = false },
                    title = { Text("Network Tips", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                    text = { Text("Home WiFi and personal hotspot work best.\nSome public networks block device-to-device connections. If that happens, use your personal hotspot.", color = TextSecondary, lineHeight = 20.sp) },
                    confirmButton = {
                        TextButton(onClick = { showNetworkTip = false }) {
                            Text("Got it", color = TextPrimary)
                        }
                    },
                    containerColor = SurfaceDark
                )
            }

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
                    Text("Scan QR Code", fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (showManualEntry) {
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = { manualIp = it },
                        label = { Text("Sender IP (e.g. 192.168.1.5)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val ip = manualIp.trim()
                            if (ip.isNotEmpty()) { showManualEntry = false; connectToIp(ip) }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(0.5f),
                            unfocusedBorderColor = Color.White.copy(0.2f),
                            focusedLabelColor = Color.White.copy(0.6f),
                            unfocusedLabelColor = Color.White.copy(0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val ip = manualIp.trim()
                            if (ip.isNotEmpty()) { showManualEntry = false; connectToIp(ip) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    TextButton(onClick = { showManualEntry = true }) {
                        Text("Enter IP manually", color = Color.White.copy(0.4f), fontSize = 13.sp)
                    }
                }
            }
        }

        // Declared after the scrollable Column so it stays on top for touch
        // handling — otherwise the Column's own bounds (which grow tall once
        // status/progress/received-files text stack up) can end up overlapping
        // and swallowing taps meant for this button.
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}
