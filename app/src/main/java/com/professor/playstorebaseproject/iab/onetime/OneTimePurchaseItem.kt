package com.professor.playstorebaseproject.iab.onetime

import com.android.billingclient.api.ProductDetails
import com.professor.playstorebaseproject.iab.ProductItem

class OneTimePurchaseItem(productDetails: ProductDetails) : ProductItem(productDetails) {
    var purchasedItem: PurchasedItem? = null
    var price = productDetails.oneTimePurchaseOfferDetails!!.formattedPrice
}