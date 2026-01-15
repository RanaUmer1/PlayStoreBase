package com.professor.pdfconverter.remoteconfig.data

import com.professor.pdfconverter.remoteconfig.RemoteConfigKeys


data class AssetsConfigData(
    val category: String = "",
    val animal: String = "",
    val animalDetails: String = "",
) {
    fun getString(key: String): String {
        return when (key) {

            else -> ""
        }
    }
}
