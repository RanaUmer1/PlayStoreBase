package com.professor.playstorebaseproject.iab.interfaces

import com.professor.playstorebaseproject.iab.ProductItem

interface PurchaseResponse
{
    fun isAlreadyOwned()
    fun userCancelled()
    fun ok(productItem: ProductItem)
    fun error(error: String)
}