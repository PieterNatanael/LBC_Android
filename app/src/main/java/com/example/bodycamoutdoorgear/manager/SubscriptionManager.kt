package com.example.bodycamoutdoorgear.manager

import com.example.bodycamoutdoorgear.i18n.tr
import android.app.Activity
import android.content.Context
import android.os.Build
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.TimeZone

class SubscriptionManager(private val context: Context) {

    companion object {
        const val PRODUCT_MONTHLY = "lbc_premium"

        val ALL_PRODUCTS = listOf(PRODUCT_MONTHLY)

        // April 18, 2026 UTC — grandfather cutoff (matches iOS)
        private val GRANDFATHER_CUTOFF_MS: Long = run {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.set(2026, Calendar.APRIL, 18, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _isGrandfathered = MutableStateFlow(false)
    val isGrandfathered: StateFlow<Boolean> = _isGrandfathered.asStateFlow()

    // All three available products (weekly / monthly / yearly)
    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val isUnlocked: Boolean
        get() = _isSubscribed.value || _isGrandfathered.value

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result: BillingResult, purchases: List<Purchase>? ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase -> handlePurchase(purchase) }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun start() {
        checkGrandfatherStatus()
        connectBilling()
    }

    private fun checkGrandfatherStatus() {
        try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            _isGrandfathered.value = info.firstInstallTime < GRANDFATHER_CUTOFF_MS
        } catch (e: Exception) {
            _isGrandfathered.value = false
        }
    }

    private fun connectBilling() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    queryProducts()
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result: BillingResult, purchases: List<Purchase> ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val active = purchases.firstOrNull { p ->
                    p.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isSubscribed.value = active != null
                if (active != null && !active.isAcknowledged) acknowledgePurchase(active)
            }
        }
    }

    private fun queryProducts() {
        val productList = ALL_PRODUCTS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        billingClient.queryProductDetailsAsync(params) { result: BillingResult, queryResult: QueryProductDetailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val sorted = queryResult.productDetailsList.sortedBy { pd ->
                    ALL_PRODUCTS.indexOf(pd.productId)
                }
                _products.value = sorted
            }
        }
    }

    fun purchase(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails
            ?.firstOrNull()?.offerToken ?: run {
            _errorMessage.value = tr("Offer not available. Try again.")
            return
        }
        val productDetailsParams = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParams)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    fun restorePurchases() {
        queryPurchases()
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            _isSubscribed.value = true
            if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { _: BillingResult -> }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
