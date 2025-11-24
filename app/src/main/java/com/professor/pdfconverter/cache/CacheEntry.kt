package com.professor.pdfconverter.cache

data class CacheEntry<T>(
    val data: List<T>,
    val timestamp: Long // time in milliseconds
)
