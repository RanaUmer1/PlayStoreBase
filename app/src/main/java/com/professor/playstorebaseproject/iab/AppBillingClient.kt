package com.professor.playstorebaseproject.iab

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import com.professor.playstorebaseproject.Constants
import com.professor.playstorebaseproject.iab.interfaces.ConnectResponse
import com.professor.playstorebaseproject.iab.interfaces.PurchaseResponse
import com.professor.playstorebaseproject.iab.interfaces.QueryResponse
import com.professor.playstorebaseproject.iab.onetime.OneTimePurchaseItem
import com.professor.playstorebaseproject.iab.subscription.SubscribedItem
import com.professor.playstorebaseproject.iab.subscription.SubscriptionItem

/**
 * Created by Ehtasham Abbas on 06 September,2023
 * Senior Android Software Engineer
 */
class AppBillingClient {
    private lateinit var billingClient: BillingClient
    private var lastItemRequestedForPurchase: ProductItem? = null

    fun connect(
        context: Context,
        connectResponse: ConnectResponse,
        purchaseResponse: PurchaseResponse
    ) {
        billingClient = BillingClient
            .newBuilder(context)
            // Purchase listener
            .setListener { billingResult: BillingResult, purchases: MutableList<Purchase>? ->


                if (billingResult.responseCode != OK) {
                    when (billingResult.responseCode) {
                        ITEM_ALREADY_OWNED -> purchaseResponse.isAlreadyOwned()
                        USER_CANCELED -> purchaseResponse.userCancelled()
                    }
                    return@setListener
                }

                //////////////////////////////////////////////////////////////////////////////////
                purchases?.forEach { purchase ->
                    // Just to be on safe side, we'll acknowledge every purchased/subscribed item.
                    if (!purchase.isAcknowledged) {
                        billingClient.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken).build()
                        ) {
                            Log.d("PurchaseItem Ack=>", "isAcknowledged$purchase")
                        }
                    }

                    lastItemRequestedForPurchase?.let {


                        for (purchaseSku in purchase.products) {
                            if (purchaseSku == it.sku) {
                                (it as SubscriptionItem).subscribedItem = SubscribedItem(
                                    purchaseSku,
                                    purchase.purchaseTime,
                                    purchase.purchaseToken
                                )
                            }
                            Log.d("PurchaseItem", "lastItemRequestedForPurchase$purchase")
                            purchaseResponse.ok(it)
                            lastItemRequestedForPurchase = null
                            // Break the loop when you're done
                            return@forEach
//                            }
                        }

                    }
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                connectResponse.disconnected()
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                /*  queryPurchases(skuToGet, object : QueryResponse<OneTimePurchaseItem> {
                      override fun ok(oneTimePurchaseItems: List<OneTimePurchaseItem>) {*/

                querySubscriptions(object : QueryResponse<SubscriptionItem> {
                    override fun ok(subscriptionItems: List<SubscriptionItem>) {
                        // Check if any item is bought or subscribed to, then acknowledge it.
                        /*   for (item in oneTimePurchaseItems) {
                               billingClient.acknowledgePurchase(
                                   AcknowledgePurchaseParams.newBuilder().setPurchaseToken(
                                       item.purchasedItem?.purchaseToken ?: continue
                                   ).build()
                               ) {}
                           }*/
                        for (item in subscriptionItems) {
                            billingClient.acknowledgePurchase(
                                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(
                                    item.subscribedItem?.purchaseToken ?: continue
                                ).build()
                            ) {
                                Log.d("PurchaseItem", "" + it.responseCode)
                            }
                        }
                        //connectResponse.ok(oneTimePurchaseItems, subscriptionItems)
                        //connectResponse.ok(subscriptionItems)
                        connectResponse.ok(subscriptionItems)
                    }

                    override fun error(responseCode: Int) {
                        log(responseCode)
                        when (responseCode) {
                            BILLING_UNAVAILABLE -> connectResponse.billingUnavailable()
                            DEVELOPER_ERROR -> connectResponse.developerError()
                            ERROR -> connectResponse.error()
                            FEATURE_NOT_SUPPORTED -> connectResponse.featureNotSupported()
                            ITEM_UNAVAILABLE -> connectResponse.itemUnavailable()
                            SERVICE_DISCONNECTED -> connectResponse.serviceDisconnected()
                            SERVICE_UNAVAILABLE -> connectResponse.serviceUnavailable()
                        }
                    }
                }, context)

            }

        })

    }

    private fun querySubscriptions(
        response: QueryResponse<SubscriptionItem>,
        context: Context
    ) {
        val positiveButtonClick = { dialog: DialogInterface, which: Int ->
            val i = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/account/subscriptions")
            )
            context.startActivity(i)
        }

        val productList =
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(Constants.SKU_SUBSCRIPTION_WEEKLY)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(Constants.SKU_SUBSCRIPTION_MONTHLY)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)
        billingClient.queryProductDetailsAsync(params.build()) { billingResult, productDetailsList ->

            if (billingResult.responseCode != OK) {
                response.error(billingResult.responseCode)
//                return@queryProductDetailsAsync
//                val dialogBuilder = AlertDialog.Builder(context)
//                dialogBuilder.setMessage(
//                    "There is a problem with your subscription. Click CONTINUE " +
//                            "to go to the Google Play subscription settings to fix your payment method."
//                )
//                    .setCancelable(true)
//                    .setPositiveButton(
//                        "Continue",
//                        DialogInterface.OnClickListener(function = positiveButtonClick)
//                    )
//                    .setNegativeButton("Cancel") { dialog, id ->
//                        dialog.cancel()
//                    }
//                val alert = dialogBuilder.create()
//                alert.show()
            }
            val skuSubscriptionItems = mutableListOf<SubscriptionItem>()

            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { result, purchaseList ->
                if (result.responseCode != OK) {
                    response.error(result.responseCode)
                    return@queryPurchasesAsync
                }
                ////////////////////////////////////////////////////////////////////


                ///////////////////////////////////////////////////////////////
                for (productDetails in productDetailsList.productDetailsList) {
                    var isItemNotPurchased = true

                    purchaseList.forEach() { purchase ->
                        for (product in purchase.products) {
                            if (productDetails.productId == product) {
                                isItemNotPurchased = false
                                Log.d("PurchaseItem ->", "query message" + purchase.toString())
                                skuSubscriptionItems.add(
                                    SubscriptionItem(productDetails)
                                        .apply {
                                            subscribedItem = SubscribedItem(
                                                product,
                                                purchase.purchaseTime,
                                                purchase.purchaseToken
                                            )
                                        }
                                )
                                return@forEach
                            }
                        }


                    }
//                    if (isItemNotPurchased && productDetails.productId == productId) {
                        skuSubscriptionItems.add(SubscriptionItem(productDetails))
//                    }
                }
                response.ok(skuSubscriptionItems)
            }
        }
    }


    fun purchaseSkuItem(baseActivity: Activity, productItem: ProductItem): Boolean {
        if (!billingClient.isReady) {
            return false
        }
        var offerToken: String? = null

        if (productItem is SubscriptionItem) {
            offerToken = productItem.offerToken ?: return false
        }
        val billingParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productItem.productDetails)
        offerToken?.let {
            billingParams.setOfferToken(it)
        }
        val productDetailsParamsList = listOf(billingParams.build())
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList).build()
        val billingResult = billingClient.launchBillingFlow(baseActivity, billingFlowParams)
        if (billingResult.responseCode == OK) {
            lastItemRequestedForPurchase = productItem
        }
        return billingResult.responseCode == OK
    }

    private fun consumeSkuItem(
        oneTimePurchaseItem: OneTimePurchaseItem,
        listener: OnConsumeListener
    ) {
        val consumeParam = ConsumeParams
            .newBuilder()
            .setPurchaseToken(oneTimePurchaseItem.purchasedItem!!.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParam) { result, _ ->

            if (result.responseCode == OK) {
                listener.onSuccessfullyConsumed()
            } else {
                listener.onConsumeError(
                    when (result.responseCode) {
                        SERVICE_TIMEOUT -> "Timed out, please try again"
                        else -> "Unknown error, please try again"
                    }
                )
            }
        }
    }

    private fun log(responseCode: Int) {
        val message = when (responseCode) {
            FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
            SERVICE_DISCONNECTED -> "SERVICE_DISCONNECTED"
            USER_CANCELED -> "USER_CANCELED"
            SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
            BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
            ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
            DEVELOPER_ERROR -> "DEVELOPER_ERROR"
            ERROR -> "ERROR"
            ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
            ITEM_NOT_OWNED -> "ITEM_NOT_OWNED"
            else -> responseCode.toString()
        }
    }

    interface OnConsumeListener {
        fun onSuccessfullyConsumed();
        fun onConsumeError(errorMessage: String)
    }
}