package com.example.bodycamoutdoorgear.giveaway

import com.example.bodycamoutdoorgear.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val LegalYellow = Color(0xFFFFD84D)
private val LegalGrey = Color(0xFF2E2E2E)
private val LegalText = Color(0xFFE0E0E0)

private val NOT_ALLOWED = listOf(
    "Medicine",
    "Weapons",
    "Alcohol",
    "Cigarettes and vapes",
    "Live animals. Please use a local adoption group.",
    "Food or drink that is past its date",
    "Recalled or unsafe items, such as used car seats and helmets",
    "Fake or stolen items",
    "Anything showing documents, addresses or other private information",
)

private val TIPS = listOf(
    "Check the background of your photo for faces, papers and screens before you share.",
    "Meet in a public place, and tell someone where you are going.",
    "Tell people the truth about damage and whether the item works.",
    "Laws differ from country to country. Follow the rules where you live.",
)

/** Shown over the card sheet the first time, before any card is made or saved. */
@Composable
fun GiveawayAgreementDialog(onAgree: () -> Unit, onDecline: () -> Unit) {
    var showRules by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(tr("Before you give something away"), color = Color.White, fontSize = 22.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Point("•", tr("You are responsible for what you give away, and for following the laws where you live."))
                    Point("•", tr("Not allowed: medicine, weapons, alcohol, cigarettes and vapes, live animals."))
                    Point("•", tr("LBC only makes the card. We don't check items, and we aren't part of any handover."))
                    Point("•", tr("Fill with AI sends a small copy of your photo to our server and an AI service (Claude, by Anthropic) to write the text. We don't keep it."))
                    Text(tr("See everything that's not allowed"), color = LegalYellow, fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { showRules = true })
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegalButton(tr("I agree"), LegalYellow, Color.Black, onAgree)
                    LegalButton(tr("Not now"), LegalGrey, Color.White, onDecline)
                }
            }
        }
    }
    if (showRules) GiveawayRulesDialog { showRules = false }
}

/** The full "What's not allowed" page. */
@Composable
fun GiveawayRulesDialog(onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(tr("What's not allowed"), color = Color.White, fontSize = 22.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                    NOT_ALLOWED.forEach { Point("✕", tr(it)) }
                    Text(tr("Good to know"), color = Color.White, fontSize = 18.sp, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
                    TIPS.forEach { Point("•", tr(it)) }
                    Text(tr("LBC makes the card only. You are responsible for what you give away."),
                        color = Color(0xFF8C8C8C), fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 10.dp))
                }
                Box(Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
                    LegalButton(tr("Close"), LegalYellow, Color.Black, onClose)
                }
            }
        }
    }
}

/** The always-visible reminder under the card, with a link to the full list. */
@Composable
fun GiveawayResponsibilityLine() {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(tr("You're responsible for what you give away."), color = Color(0xFF808080), fontSize = 11.sp,
            fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
        Text(tr("What's not allowed"), color = Color(0xFFBFBFBF), fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { open = true }.padding(vertical = 2.dp))
    }
    if (open) GiveawayRulesDialog { open = false }
}

@Composable
private fun Point(mark: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(mark, color = LegalYellow, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
        Text(text, color = LegalText, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun LegalButton(text: String, fill: Color, textColor: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(fill).clickable(onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
