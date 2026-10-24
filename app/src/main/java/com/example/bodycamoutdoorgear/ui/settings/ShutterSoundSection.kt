package com.example.bodycamoutdoorgear.ui.settings

import com.example.bodycamoutdoorgear.i18n.tr
import androidx.compose.foundation.background
import com.example.bodycamoutdoorgear.ui.theme.themedShape
import com.example.bodycamoutdoorgear.ui.theme.chromeAccentOr
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bodycamoutdoorgear.sound.ShutterSoundPlayer
import com.example.bodycamoutdoorgear.transfer.SoundMeta
import com.example.bodycamoutdoorgear.ui.transfer.SendScreen
import com.example.bodycamoutdoorgear.sound.ShutterSoundStore
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.viewmodel.SettingsViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamoutdoorgear.sound.ShutterSound

@Composable
fun ShutterSoundSection(
    selectedId: String,
    onSelect: (ShutterSound) -> Unit,
    onPreview: (ShutterSound) -> Unit,
    sounds: List<ShutterSound> = ShutterSound.builtIn,
    accent: Color = Color.White,
    canRecord: Boolean = false,
    onRecord: () -> Unit = {},
    onShare: (ShutterSound) -> Unit = {},
    onDelete: (ShutterSound) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            tr("SHUTTER SOUND"),
            color = chromeAccentOr(Color.White.copy(0.4f)),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        sounds.forEach { sound ->
            val selected = sound.id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(themedShape(6.dp))
                    .background(if (selected) chromeAccentOr(Color.White).copy(0.25f) else Color.White.copy(0.1f))
                    .border(
                        1.dp,
                        if (selected) chromeAccentOr(Color.White).copy(0.5f) else Color.White.copy(0.2f),
                        themedShape(6.dp)
                    )
                    .clickable { onSelect(sound) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sound.emoji, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    tr(sound.name),
                    modifier = Modifier.weight(1f),
                    color = if (selected) chromeAccentOr(Color.White) else Color.White.copy(0.5f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (selected) {
                    Icon(Icons.Filled.Check, contentDescription = tr("Selected"), tint = chromeAccentOr(Color.White), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                }
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = tr("Preview"),
                    tint = Color.White.copy(0.6f),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onPreview(sound) }
                )
                if (sound.isCustom) {
                    Spacer(Modifier.width(8.dp))
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = tr("More"),
                            tint = Color.White.copy(0.6f),
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { menuOpen = true }
                        )
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = SurfaceDark
                        ) {
                            DropdownMenuItem(
                                text = { Text(tr("Share via HipHop"), color = Color.White) },
                                onClick = { menuOpen = false; onShare(sound) }
                            )
                            DropdownMenuItem(
                                text = { Text(tr("Delete"), color = Color.White) },
                                onClick = { menuOpen = false; onDelete(sound) }
                            )
                        }
                    }
                }
            }
        }
        if (canRecord) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(themedShape(6.dp))
                    .background(accent)
                    .clickable { onRecord() }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = accent.readableTextColor(), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    tr("RECORD YOUR OWN"),
                    color = accent.readableTextColor(),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
        Text(
            tr("Plays at your media volume."),
            color = Color.White.copy(0.3f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

/** The whole SHUTTER SOUND block for a Settings sheet: list, recorder and delete confirmation. */
@Composable
fun ShutterSoundSettings(settingsViewModel: SettingsViewModel, accent: Color) {
    val context = LocalContext.current
    val store = remember { ShutterSoundStore.get(context) }
    val player = remember { ShutterSoundPlayer.get(context) }
    val custom by store.custom.collectAsState()
    val selectedId by settingsViewModel.shutterSoundId.collectAsState()
    var showRecorder by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<ShutterSound?>(null) }
    var toShare by remember { mutableStateOf<ShutterSound?>(null) }

    ShutterSoundSection(
        selectedId = selectedId,
        onSelect = { settingsViewModel.setShutterSound(it.id); player.play(it) },
        onPreview = { player.play(it) },
        sounds = ShutterSound.builtIn + custom,
        accent = accent,
        canRecord = custom.size < ShutterSoundStore.MAX_CUSTOM,
        onRecord = { showRecorder = true },
        onShare = { toShare = it },
        onDelete = { toDelete = it }
    )

    toShare?.let { sound ->
        Dialog(
            onDismissRequest = { toShare = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val file = store.fileFor(sound.id)
            SendScreen(
                files = listOf(file),
                onDismiss = { toShare = null },
                soundMeta = mapOf(file.name to SoundMeta(sound.name, sound.emoji))
            )
        }
    }

    if (showRecorder) {
        RecordShutterSoundDialog(
            accent = accent,
            onDismiss = { showRecorder = false },
            onSaved = { sound ->
                settingsViewModel.setShutterSound(sound.id)
                player.play(sound)
                showRecorder = false
            }
        )
    }

    toDelete?.let { sound ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = SurfaceDark,
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(0.7f),
            title = { Text(tr("Delete this sound?")) },
            text = { Text(tr("This cannot be undone.")) },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedId == sound.id) settingsViewModel.setShutterSound(ShutterSound.CAT.id)
                    store.remove(sound)
                    toDelete = null
                }) { Text(tr("Delete"), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(tr("Cancel"), color = Color.White.copy(0.7f)) }
            }
        )
    }
}
