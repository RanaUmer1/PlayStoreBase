package com.professor.playstorebaseproject.remoteconfig.data

import com.professor.playstorebaseproject.remoteconfig.RemoteConfigKeys


data class AssetsConfigData(
    val category: String = "",
    val animal: String = "",
    val animalDetails: String = "",
) {
    fun getString(key: String): String {
        return when (key) {
            RemoteConfigKeys.ANIMAL_CATEGORY_JSON -> category
            RemoteConfigKeys.ANIMAL_JSON -> animal
            RemoteConfigKeys.ANIMAL_DETAILS_JSON -> animalDetails

            else -> ""
        }
    }
}
