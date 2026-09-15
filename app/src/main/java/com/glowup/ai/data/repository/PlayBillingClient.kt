package com.glowup.ai.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Singleton
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Play owns the payment UI. Entitlements are verified by the backend. */
@Singleton
class PlayBillingClient @Inject constructor(@ApplicationContext context: Context) {
    var onPurchases: (List<Purchase>) -> Unit = {}
    var onMessage: (String?) -> Unit = {}

    private val connectionMutex = Mutex()
    private val client = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (!purchases.isNullOrEmpty()) onPurchases(purchases)
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> onMessage(null)
                else -> onMessage("Google Play checkout is unavailable. Please try again.")
            }
        }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    suspend fun connect() {
        if (client.isReady) return
        connectionMutex.withLock {
            if (client.isReady) return
            suspendCancellableCoroutine<Unit> { continuation ->
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (!continuation.isActive) return
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    "Google Play billing is unavailable on this device.",
                                ),
                            )
                        }
                    }

                    override fun onBillingServiceDisconnected() = Unit
                })
            }
        }
    }

    suspend fun products(ids: List<String>): List<ProductDetails> {
        if (ids.isEmpty()) return emptyList()
        connect()
        return suspendCancellableCoroutine { continuation ->
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    ids.distinct().map {
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(it)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    },
                )
                .build()
            client.queryProductDetailsAsync(params) { result, queryResult ->
                if (continuation.isActive) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(queryResult.productDetailsList)
                    } else {
                        continuation.resumeWithException(
                            IllegalStateException("Unable to load Google Play plans."),
                        )
                    }
                }
            }
        }
    }

    suspend fun restore(): List<Purchase> {
        connect()
        return suspendCancellableCoroutine { continuation ->
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { result, purchases ->
                if (continuation.isActive) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) continuation.resume(purchases)
                    else continuation.resumeWithException(IllegalStateException("Unable to restore Google Play purchases."))
                }
            }
        }
    }

    /**
     * Re-queries Play immediately before opening checkout. ProductDetails are deliberately not
     * cached because their offer tokens can become stale and make launchBillingFlow fail.
     */
    suspend fun launch(
        activity: Activity,
        userId: String,
        productId: String,
        offerToken: String,
    ) {
        val product = products(listOf(productId)).firstOrNull { it.productId == productId }
        val offerStillAvailable = product?.subscriptionOfferDetails.orEmpty()
            .any { it.offerToken == offerToken }
        if (product == null || !offerStillAvailable) {
            onMessage("This Google Play offer is no longer available. Please reload plans.")
            return
        }

        val account = MessageDigest.getInstance("SHA-256")
            .digest(userId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val params = BillingFlowParams.newBuilder()
            .setObfuscatedAccountId(account)
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()
        val result = withContext(Dispatchers.Main.immediate) {
            client.launchBillingFlow(activity, params)
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            onMessage("Google Play checkout could not start. Please try again.")
        }
    }

    fun close() = client.endConnection()
}
