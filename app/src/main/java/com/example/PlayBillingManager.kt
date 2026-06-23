package com.example

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayBillingManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPremiumStatusChanged: (Boolean) -> Unit
) {
    private val TAG = "PlayBillingManager"
    
    // Default standard Premium ID
    val PREMIUM_SKU = "com.mds.loanmath.premium"

    private var billingClient: BillingClient? = null

    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState = _connectionState.asStateFlow()

    private val _premiumPurchased = MutableStateFlow(false)
    val premiumPurchased = _premiumPurchased.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val availableProducts = _availableProducts.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Billing: User canceled the purchase flow.")
        } else {
            Log.e(TAG, "Billing failure response: Code ${billingResult.responseCode}, Message: ${billingResult.debugMessage}")
        }
    }

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        _connectionState.value = "Initializing"
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        startConnection()
    }

    fun startConnection() {
        billingClient?.let { client ->
            _connectionState.value = "Connecting"
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _connectionState.value = "Connected"
                        Log.d(TAG, "Google Play Billing client connected successfully.")
                        queryActivePurchases()
                        queryAvailableSkuDetails()
                    } else {
                        _connectionState.value = "Error: ${billingResult.debugMessage}"
                        Log.e(TAG, "Google Play Billing setup failed: ${billingResult.debugMessage}")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _connectionState.value = "Disconnected"
                    Log.d(TAG, "Play Billing connection lost. Retrying or reconnecting...")
                }
            })
        }
    }

    fun queryActivePurchases() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        client.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var isPremiumOwned = false
                for (purchase in purchases) {
                    if (purchase.products.contains(PREMIUM_SKU) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        isPremiumOwned = true
                        acknowledgePurchaseIfNeeded(purchase)
                    }
                }
                _premiumPurchased.value = isPremiumOwned
                onPremiumStatusChanged(isPremiumOwned)
                Log.d(TAG, "Active purchases queried. Premium owned: $isPremiumOwned")
            } else {
                Log.e(TAG, "Failed querying active purchases: ${billingResult.debugMessage}")
            }
        }
    }

    private fun queryAvailableSkuDetails() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_SKU)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _availableProducts.value = productDetailsList
                Log.d(TAG, "Loaded products list: ${productDetailsList.size} items.")
            } else {
                Log.e(TAG, "Failed loading available product details: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, onFallbackSimulation: () -> Unit) {
        val client = billingClient
        if (client == null || !client.isReady) {
            Log.e(TAG, "Play Billing Client is not connected or initialized. Resorting to safe simulator...")
            onFallbackSimulation()
            return
        }

        val details = _availableProducts.value.find { it.productId == PREMIUM_SKU }
        if (details == null) {
            Log.e(TAG, "Product '$PREMIUM_SKU' details not found from Play Console yet. Initiating simulation fallback for test builds...")
            onFallbackSimulation()
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        client.launchBillingFlow(activity, flowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            _premiumPurchased.value = true
            onPremiumStatusChanged(true)
            acknowledgePurchaseIfNeeded(purchase)
        }
    }

    private fun acknowledgePurchaseIfNeeded(purchase: Purchase) {
        val client = billingClient ?: return
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            client.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Premium purchase acknowledged successfully!")
                } else {
                    Log.e(TAG, "Failed to acknowledge premium purchase: ${billingResult.debugMessage}")
                }
            }
        }
    }
}
