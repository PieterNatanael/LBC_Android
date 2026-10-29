package com.example.bodycamoutdoorgear.ui.disclaimer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamoutdoorgear.R
import com.example.bodycamoutdoorgear.i18n.Tr
import com.example.bodycamoutdoorgear.i18n.tr

// The green of the logo itself, so the button looks part of the same picture.
private val LogoGreen = Color(0xFF7D937C)

/**
 * The one-time first-run screen: a welcome, one button, and links to the Privacy Policy and
 * Terms of Use, where the detail lives. Nothing is lost by keeping it short: Android asks for
 * each permission when it is needed, the Fill with AI consent appears the first time that
 * button is tapped, and the data loss notes are in the Terms of Use.
 */
@Composable
fun DisclaimerScreen(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    // Pure black, like the logo picture's own background, so its edge disappears.
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Image(
                painter = painterResource(R.drawable.lbc_welcome),
                contentDescription = null,
                modifier = Modifier.size(300.dp),
            )

            Text(
                text = tr("Welcome to LBC"),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                text = "Low Battery Camera",
                color = Color(0xFFA6A6A6),
                fontSize = 17.sp,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(LogoGreen)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 17.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(tr("Continue"), color = Color(0xFF0A0A0A), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = tr("By continuing you accept our"),
                color = Color(0xFF999999),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp),
            )
            Row(
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tr("Privacy Policy"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { uriHandler.openUri(Tr.privacyUrl) },
                )
                Text("·", color = Color(0xFF999999), fontSize = 13.sp)
                Text(
                    tr("Terms of Use"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { uriHandler.openUri(Tr.termsUrl) },
                )
            }
        }
    }
}
