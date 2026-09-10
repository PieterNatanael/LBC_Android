package com.example.bodycamoutdoorgear.ui.paywall

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.asImageBitmap
import com.example.bodycamoutdoorgear.R
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
import com.example.bodycamoutdoorgear.ui.theme.BackgroundDark
import com.example.bodycamoutdoorgear.ui.theme.BorderColor
import com.example.bodycamoutdoorgear.ui.theme.ButtonFace
import com.example.bodycamoutdoorgear.ui.theme.CompassGold
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextPrimary
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary

private val FEATURES = listOf(
    "Save recordings to Photos",
    "Share recordings",
    "All quality levels & settings",
    "Free updates"
)

private const val PRIVACY_URL = "https://github.com/PieterNatanael/Low-Battery-Camera/tree/main"
private const val TERMS_URL   = "https://github.com/PieterNatanael/Low-Battery-Camera/blob/main/terms.md"

private data class PlanLabel(val billing: String, val badge: String?)

private val PLAN_LABELS = mapOf(
    "lbc_premium" to PlanLabel("billed monthly · auto-renews", null)
)

@Composable
fun PaywallScreen(
    subscriptionManager: SubscriptionManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val products    by subscriptionManager.products.collectAsState()
    val isLoading   by subscriptionManager.isLoading.collectAsState()
    val errorMessage by subscriptionManager.errorMessage.collectAsState()
    val isGrandfathered by subscriptionManager.isGrandfathered.collectAsState()

    var selectedProduct by remember { mutableStateOf<ProductDetails?>(null) }

    // Auto-select monthly (only plan available)
    if (selectedProduct == null && products.isNotEmpty()) {
        selectedProduct = products.firstOrNull { it.productId == "lbc_premium" }
            ?: products.first()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = BackgroundDark) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close button
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // App logo + title
                val ctx = LocalContext.current
                val iconBitmap = remember {
                    BitmapFactory.decodeResource(ctx.resources, R.mipmap.ic_launcher_round)
                }
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "LOW BATTERY CAMERA PRO",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Unlock the full app",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(Modifier.height(20.dp))

                // Features
                FEATURES.forEach { feature ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = CompassGold, modifier = Modifier.size(18.dp))
                        Text(feature, color = TextPrimary, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(Modifier.height(20.dp))

                // Plan selector
                if (products.isEmpty()) {
                    CircularProgressIndicator(color = CompassGold, modifier = Modifier.size(28.dp))
                } else {
                    products.forEach { product ->
                        PlanRow(
                            product = product,
                            isSelected = product.productId == selectedProduct?.productId,
                            planLabel = PLAN_LABELS[product.productId],
                            onSelect = { selectedProduct = product }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Subscribe button
                Button(
                    onClick = {
                        val activity = context as? Activity ?: return@Button
                        selectedProduct?.let { subscriptionManager.purchase(activity, it) }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = ButtonFace),
                    enabled = selectedProduct != null && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ButtonFace)
                    } else {
                        Text("SUBSCRIBE", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Billing disclosure
                val selectedPrice = selectedProduct?.subscriptionOfferDetails
                    ?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                Text(
                    text = buildString {
                        if (selectedPrice != null) append("$selectedPrice/month. ")
                        append("Payment charged to your Google Play account at purchase confirmation. ")
                        append("Subscription auto-renews monthly unless cancelled at least 24 hours before the renewal date. ")
                        append("Manage or cancel anytime in Google Play settings.")
                    },
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(10.dp))

                TextButton(onClick = subscriptionManager::restorePurchases) {
                    Text("Restore Purchases", color = TextSecondary, fontSize = 13.sp)
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMessage ?: "", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                }

                if (isGrandfathered) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Bought Low Battery Camera before April 2026? Your full access is restored automatically — no action needed.",
                        color = CompassGold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(Modifier.height(12.dp))

                // Privacy Policy & Terms
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
                    }) {
                        Text("Privacy Policy", color = TextSecondary, fontSize = 11.sp)
                    }
                    Text("·", color = BorderColor)
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL)))
                    }) {
                        Text("Terms of Use", color = TextSecondary, fontSize = 11.sp)
                    }
                }

                // Developer credit
                Text(
                    text = "Low Battery Camera (LBC)\nDeveloped by Three Dollar",
                    color = TextSecondary.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PlanRow(
    product: ProductDetails,
    isSelected: Boolean,
    planLabel: PlanLabel?,
    onSelect: () -> Unit
) {
    val price = product.subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.lastOrNull()
        ?.formattedPrice ?: "—"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SurfaceDark else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) CompassGold else BorderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = price,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // "BEST VALUE" badge
                    if (planLabel?.badge != null) {
                        Text(
                            text = planLabel.badge,
                            color = CompassGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .border(1.dp, CompassGold, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                if (planLabel?.billing != null) {
                    Text(
                        text = planLabel.billing,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            // Radio indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .border(
                        width = if (isSelected) 0.dp else 1.5.dp,
                        color = if (isSelected) Color.Transparent else BorderColor,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .background(if (isSelected) CompassGold else Color.Transparent)
            )
        }
    }
}
