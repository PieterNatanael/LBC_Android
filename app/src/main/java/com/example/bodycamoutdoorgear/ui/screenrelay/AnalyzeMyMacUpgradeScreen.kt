package com.example.bodycamoutdoorgear.ui.screenrelay

import com.example.bodycamoutdoorgear.i18n.tr
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.billingclient.api.ProductDetails
import com.example.bodycamoutdoorgear.manager.SubscriptionManager
import com.example.bodycamoutdoorgear.ui.paywall.PaywallScreen
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.BorderColor
import com.example.bodycamoutdoorgear.ui.theme.ButtonFace
import com.example.bodycamoutdoorgear.ui.theme.CompassGold
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary

/**
 * Plan picker for Analyze my Mac's AI-quota tiers. Pro keeps its existing
 * paywall (PaywallScreen, unchanged); Plus and Max are single weekly
 * products with no period choice, so they get a plain inline subscribe
 * button here instead.
 */
@Composable
fun AnalyzeMyMacUpgradeScreen(
    subscriptionManager: SubscriptionManager,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val products by subscriptionManager.products.collectAsState()
    val isLoading by subscriptionManager.isLoading.collectAsState()
    val errorMessage by subscriptionManager.errorMessage.collectAsState()
    var showProPaywall by remember { mutableStateOf(false) }

    val currentTier = subscriptionManager.analyzeMyMacTier
    val plusProduct = products.firstOrNull { it.productId == SubscriptionManager.PRODUCT_PLUS }
    val maxProduct = products.firstOrNull { it.productId == SubscriptionManager.PRODUCT_MAX }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = tr("Close"), tint = TextSecondary)
                    }
                }

                Text(
                    text = tr("ANALYZE MY MAC PLANS"),
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tr("More requests, and a stronger model at the higher tiers."),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                TierCard(
                    name = tr("Free"), price = "—", quota = tr("3 analyses / month"), model = "Haiku",
                    isCurrent = currentTier == "free", isBest = false, onChoose = null, isLoading = false
                )
                Spacer(Modifier.height(12.dp))
                TierCard(
                    name = tr("LBC Pro"), price = tr("from \$1 / month"), quota = tr("100 analyses / month"), model = "Haiku",
                    isCurrent = currentTier == "pro", isBest = false,
                    onChoose = { showProPaywall = true }, isLoading = false
                )
                Spacer(Modifier.height(12.dp))
                TierCard(
                    name = tr("LBC Plus"),
                    price = plusProduct?.let { price(it) } ?: "$3 / week",
                    quota = tr("50 analyses / week"), model = "Sonnet",
                    isCurrent = currentTier == "plus", isBest = false,
                    onChoose = {
                        val activity = context as? Activity ?: return@TierCard
                        if (plusProduct != null) subscriptionManager.purchase(activity, plusProduct)
                        else subscriptionManager.reportUnavailable()
                    },
                    isLoading = isLoading
                )
                Spacer(Modifier.height(12.dp))
                TierCard(
                    name = tr("LBC Max"),
                    price = maxProduct?.let { price(it) } ?: "$9 / week",
                    quota = tr("150 analyses / week"), model = "Sonnet",
                    isCurrent = currentTier == "max", isBest = true,
                    onChoose = {
                        val activity = context as? Activity ?: return@TierCard
                        if (maxProduct != null) subscriptionManager.purchase(activity, maxProduct)
                        else subscriptionManager.reportUnavailable()
                    },
                    isLoading = isLoading
                )

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(Modifier.height(12.dp))

                Text(
                    text = tr(
                        "Payment charged to your Google Play account at purchase confirmation. " +
                            "Subscriptions auto-renew for the same price and period unless cancelled at least " +
                            "24 hours before renewal. Manage or cancel anytime in Google Play settings."
                    ),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(10.dp))
                TextButton(onClick = subscriptionManager::restorePurchases) {
                    Text(tr("Restore Purchases"), color = TextSecondary, fontSize = 13.sp)
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMessage ?: "", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showProPaywall) {
        PaywallScreen(subscriptionManager = subscriptionManager, onDismiss = { showProPaywall = false })
    }
}

private fun price(product: ProductDetails): String =
    product.subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.lastOrNull()
        ?.formattedPrice ?: "—"

@Composable
private fun TierCard(
    name: String,
    price: String,
    quota: String,
    model: String,
    isCurrent: Boolean,
    isBest: Boolean,
    onChoose: (() -> Unit)?,
    isLoading: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) CompassGold else BorderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (isBest) {
                    Text(
                        text = tr("BEST"),
                        color = CompassGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .border(1.dp, CompassGold, RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(price, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(6.dp))
            Text("$quota · $model", color = TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            if (isCurrent) {
                Text(
                    text = tr("Your current plan"),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            } else if (onChoose != null) {
                Button(
                    onClick = onChoose,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ButtonFace),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ButtonFace)
                    } else {
                        Text(tr("Choose this plan"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
