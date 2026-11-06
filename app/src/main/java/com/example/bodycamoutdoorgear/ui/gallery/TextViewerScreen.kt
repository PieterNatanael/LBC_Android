package com.example.bodycamoutdoorgear.ui.gallery

import com.example.bodycamoutdoorgear.i18n.tr
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamoutdoorgear.data.VideoItem
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.RecordRed
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary

/**
 * Full-screen viewer for a saved "Analyze my Mac" result (or any other
 * plain-text gallery entry). Much simpler than PhotoViewerScreen/
 * VideoPlayerScreen — no Private Gallery move, no scheduling — but keeps
 * the same (onDismiss, onDeleted) shape so the gallery pager can host all
 * kinds of pages identically.
 */
@Composable
fun TextViewerScreen(
    item: VideoItem,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val text = remember(item.file) { item.file.readText() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 64.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = tr("Close"), tint = TextPrimary)
            }
            Row {
                IconButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                }) {
                    Icon(Icons.Default.IosShare, contentDescription = tr("Share"), tint = TextPrimary)
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = tr("Delete"), tint = RecordRed)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(tr("Delete this note?"), color = TextPrimary) },
            text = { Text(tr("This cannot be undone."), color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    item.file.delete()
                    onDeleted()
                }) { Text(tr("Delete"), color = RecordRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(tr("Cancel"), color = TextPrimary) }
            },
            containerColor = SurfaceDark,
        )
    }
}
