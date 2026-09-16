package com.example.bodycamoutdoorgear.ui.transfer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import com.example.bodycamoutdoorgear.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamoutdoorgear.transfer.QrCodeUtil
import com.example.bodycamoutdoorgear.transfer.TransferServer
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary
import java.io.File

@Composable
fun SendScreen(
    files: List<File>,
    onDismiss: () -> Unit
) {
    var showNetworkTip by remember { mutableStateOf(false) }
    val ip = remember { TransferServer.getLocalIp() }
    val qrBitmap = remember(ip) {
        ip?.let {
            val url = TransferServer.buildTransferUrl(it)
            QrCodeUtil.generate(url, 512)
        }
    }

    DisposableEffect(files) {
        TransferServer.start(files)
        onDispose { TransferServer.stop() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.hiphop),
                contentDescription = "HipHop",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Text(
                "HipHop",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "HipHop transfers files directly between your devices. No internet needed, just WiFi.\nMake sure both devices are on the same WiFi network.\nOn the other device, open LBC, tap HipHop, then scan this code.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
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

            if (qrBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(SurfaceDark, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No WiFi network detected", color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                }
            }

            if (ip != null) {
                Text(
                    "Your IP: $ip",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${files.size} file${if (files.size != 1) "s" else ""} ready to send",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Waiting for receiver to connect...",
                color = Color.White.copy(0.5f),
                fontSize = 12.sp
            )
        }

        // Declared after the Column so it stays on top for touch handling —
        // see ReceiveScreen for why this ordering matters.
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}
