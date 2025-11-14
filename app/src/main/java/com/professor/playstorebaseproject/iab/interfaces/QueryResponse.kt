package com.professor.playstorebaseproject.iab.interfaces

import com.professor.playstorebaseproject.iab.ProductItem

interface QueryResponse<in T : ProductItem>
{
    fun error(responseCode: Int)
    fun ok(skuItems: List<T>)
}