package com.example.bodycamoutdoorgear.ui.camera

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamoutdoorgear.giveaway.GiveawayCardStrings
import com.example.bodycamoutdoorgear.giveaway.GiveawayLanguage
import com.example.bodycamoutdoorgear.i18n.tr
import com.example.bodycamoutdoorgear.ui.theme.LocalMarsTheme
import com.example.bodycamoutdoorgear.ui.theme.MarsChamferShape
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.sideButtonStyle
import com.example.bodycamoutdoorgear.screenrelay.ScreenRelayPairing
import com.example.bodycamoutdoorgear.viewmodel.DisplayMode

private val INK = Color(0xFF1B1A17)

/** The three modes that make a card, in the order the AI menu lists them. */
val CARD_MODES = listOf(DisplayMode.GIVEAWAY, DisplayMode.SALE, DisplayMode.SWAP)

/**
 * The colour of a card mode on the camera screen. Yellow, blue and pink stay apart from
 * each other in every kind of colour blindness. Null for modes that make no card.
 */
val DisplayMode.cardColor: Color?
    get() = when (this) {
        DisplayMode.GIVEAWAY -> Color(0xFFFFD84D)
        DisplayMode.SALE -> Color(0xFF3B82F6)
        DisplayMode.SWAP -> Color(0xFFE0559B)
        else -> null
    }

/** Text on top of [cardColor]: dark on yellow, white on blue and pink. */
val DisplayMode.onCardColor: Color get() = if (this == DisplayMode.GIVEAWAY) INK else Color.White

/** Switches the card mode from the camera screen. It is the same stored setting Settings uses. */
object CardModeSwitch {
    private fun prefs(context: Context) = context.getSharedPreferences("giveaway", Context.MODE_PRIVATE)

    /**
     * The mode after the user taps [target]: that card mode turns on, and tapping the one that is
     * already on turns card mode off, back to the mode the camera was in before (or Normal).
     */
    fun toggled(context: Context, current: DisplayMode, target: DisplayMode): DisplayMode {
        if (current == target) {
            val saved = prefs(context).getString("mode_before_card", null)
            val previous = DisplayMode.entries.firstOrNull { it.name == saved }
            return if (previous != null && !previous.makesCard) previous else DisplayMode.NORMAL
        }
        if (!current.makesCard) prefs(context).edit().putString("mode_before_card", current.name).apply()
        return target
    }
}

/** The round "AI" button next to Settings. Plain when no card mode is on; in the card's colour when one is. */
@Composable
fun AICardButton(mode: DisplayMode, accentColor: Color, isAlt: Boolean, onClick: () -> Unit) {
    val lit = mode.cardColor
    val shapeModifier = when {
        lit != null && LocalMarsTheme.current ->
            Modifier.clip(MarsChamferShape).background(lit).border(2.dp, lit, MarsChamferShape)
        lit != null -> Modifier.background(lit, CircleShape)
        else -> Modifier.sideButtonStyle(SurfaceDark, accentColor)
    }
    Box(
        modifier = Modifier.size(48.dp).then(shapeModifier).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "AI",
            color = if (lit != null) mode.onCardColor else if (isAlt) accentColor else Color.White.copy(0.85f),
            fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** A small label at the top of the camera naming the card mode that is on. Tap it to open the menu. */
@Composable
fun AICardChip(mode: DisplayMode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = mode.cardColor ?: return
    val strings = GiveawayCardStrings.current
    val title = when (mode) {
        DisplayMode.GIVEAWAY -> strings.header
        DisplayMode.SALE -> strings.saleHeader
        else -> strings.swapHeader
    }
    Text(
        title, color = mode.onCardColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, maxLines = 1, overflow = TextOverflow.Clip,
        modifier = modifier.clip(CircleShape).background(color).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** "Make a card with AI": the three card modes. The one that is on has a tick; tapping it turns card mode off. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICardMenu(
    current: DisplayMode,
    onPick: (DisplayMode) -> Unit,
    onDismiss: () -> Unit,
    onAnalyzeMyMac: () -> Unit = {},
) {
    val context = LocalContext.current
    val strings = GiveawayCardStrings.current
    var pickingLanguage by remember { mutableStateOf(false) }
    if (pickingLanguage) CardLanguagePicker(onDismiss = { pickingLanguage = false })
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1C1C1C),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tr("Make a card with AI"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = tr("Close"), tint = Color(0xFFCCCCCC))
                }
            }
            CARD_MODES.forEach { mode ->
                val title = when (mode) {
                    DisplayMode.GIVEAWAY -> tr("Giveaway")
                    DisplayMode.SALE -> tr("For sale")
                    else -> tr("Swap")
                }
                val subtitle = when (mode) {
                    DisplayMode.GIVEAWAY -> tr("Give it to a good home")
                    DisplayMode.SALE -> tr("Add your price")
                    else -> tr("Say what you are looking for")
                }
                val stamp = when (mode) {
                    DisplayMode.GIVEAWAY -> "FREE"
                    DisplayMode.SALE -> "$3"
                    else -> "⇄"
                }
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(mode) }.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(52.dp)) {
                        Box(Modifier.size(46.dp).align(Alignment.BottomStart).clip(RoundedCornerShape(10.dp)).background(mode.cardColor ?: Color.Gray))
                        Text(
                            stamp, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 1,
                            fontSize = 9.sp,
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp)
                                .clip(RoundedCornerShape(5.dp)).background(INK)
                                .border(1.dp, Color.White, RoundedCornerShape(5.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, color = Color(0xFF999999), fontSize = 13.sp)
                    }
                    if (current == mode) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = mode.cardColor ?: Color.White)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).height(1.dp).background(Color(0xFF3A3A3A)))
            Row(
                Modifier.fillMaxWidth().clickable { onDismiss(); onAnalyzeMyMac() }.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Devices, contentDescription = null, tint = Color(0xFFCCCCCC))
                }
                Column(Modifier.weight(1f)) {
                    Text(tr("Analyze my Mac"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (ScreenRelayPairing.isPaired(context)) tr("Linked") else tr("Tap to link your Mac"),
                        color = Color(0xFF999999), fontSize = 13.sp,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF808080))
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).height(1.dp).background(Color(0xFF3A3A3A)))
            Row(
                Modifier.fillMaxWidth().clickable { pickingLanguage = true }.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFFCCCCCC))
                }
                Column(Modifier.weight(1f)) {
                    Text(tr("Card language"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(GiveawayLanguage.name(GiveawayLanguage.code), color = Color(0xFF999999), fontSize = 13.sp)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF808080))
            }
        }
    }
}

/** The languages the card can be written in. The pick is saved; "Same as app language" clears it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardLanguagePicker(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1C1C1C),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tr("Card language"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = tr("Close"), tint = Color(0xFFCCCCCC))
                }
            }
            Text(tr("The card text is written in this language."), color = Color(0xFF999999), fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp))
            val usingAppLanguage = GiveawayLanguage.saved !in GiveawayLanguage.SUPPORTED
            LazyColumn {
                item {
                    LanguageRow(tr("Same as app language"), GiveawayLanguage.name(GiveawayLanguage.appCode), usingAppLanguage) {
                        GiveawayLanguage.select("")
                        onDismiss()
                    }
                }
                items(GiveawayLanguage.NAMES) { (code, name) ->
                    LanguageRow(name, null, !usingAppLanguage && GiveawayLanguage.saved == code) {
                        GiveawayLanguage.select(code)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(title: String, detail: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (detail != null) Text(detail, color = Color(0xFF999999), fontSize = 13.sp)
        }
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
    }
}

/** Zoom button for the ring. The ring holds it so the right side can hold the AI button. */
@Composable
fun ZoomRingButton(text: String, accentColor: Color, isAlt: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .sideButtonStyle(SurfaceDark, accentColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, color = if (isAlt) accentColor else Color.White.copy(0.85f),
            fontSize = if (isAlt) 14.sp else 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
        )
    }
}
