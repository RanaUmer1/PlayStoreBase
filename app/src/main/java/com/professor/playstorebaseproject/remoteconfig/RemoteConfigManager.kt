package com.professor.playstorebaseproject.remoteconfig

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.professor.playstorebaseproject.R
import com.professor.playstorebaseproject.remoteconfig.data.AdsConfigData
import com.professor.playstorebaseproject.remoteconfig.data.AssetsConfigData
import dagger.Provides
import javax.inject.Singleton

/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com

 */


object RemoteConfigManager {
    private val TAG = RemoteConfigManager::class.java.simpleName

    private var adsConfigData: AdsConfigData = AdsConfigData()
    private var assetsConfigData: AssetsConfigData = AssetsConfigData()
    private var animalSoundSplashAd = 1
    private var showLangNativeMedia: Boolean = true
    private var showOnboardingNativeMedia: Boolean = true
    private var showAnimalNativeMedia: Boolean = true
    private var disableAds: Boolean = false
    private var notificationTime = 3L

    private val firebaseRemoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setFetchTimeoutInSeconds(10)
                    .setMinimumFetchIntervalInSeconds(0)
                    .build()
            )
            setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }

    fun fetchRemoteConfig(callback: (Boolean) -> Unit) {
        firebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    parseFetchData()
                    callback(true)
                } else {
                    Log.e(TAG, "RemoteConfig fetch failed.")
                    callback(false)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "RemoteConfig fetch error: ${it.message}")
                callback(false)
            }

    }


    private fun parseFetchData() {

        //Ads Config RemoteConfig
        val json = firebaseRemoteConfig.getString(RemoteConfigKeys.ANIMAL_SOUND_AD_CONFIG)
        adsConfigData = try {
            if (json.isNotBlank()) {
                Log.e(TAG, "remote config: ")
                Gson().fromJson(json, AdsConfigData::class.java)
            } else {
                Log.e(TAG, "local... ")
                AdsConfigData()
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to parse RemoteConfigData: ${ex.message}")
            AdsConfigData()

        }

        //set ads media value
        showLangNativeMedia =
            firebaseRemoteConfig.getBoolean(RemoteConfigKeys.SHOW_LANG_NATIVE_MEDIA)

        showOnboardingNativeMedia =
            firebaseRemoteConfig.getBoolean(RemoteConfigKeys.SHOW_ONBOARDING_NATIVE_MEDIA)

        showAnimalNativeMedia =
            firebaseRemoteConfig.getBoolean(RemoteConfigKeys.SHOW_ANIMAL_NATIVE_MEDIA)

        disableAds =
            firebaseRemoteConfig.getBoolean(RemoteConfigKeys.DISABLE_ADS)

        notificationTime =
            firebaseRemoteConfig.getLong(RemoteConfigKeys.NOTIFICATION_TIME)
        //Assets RemoteConfig

        assetsConfigData = AssetsConfigData(
            category = firebaseRemoteConfig.getString(RemoteConfigKeys.ANIMAL_CATEGORY_JSON),
            animal = firebaseRemoteConfig.getString(RemoteConfigKeys.ANIMAL_JSON),
            animalDetails = firebaseRemoteConfig.getString(RemoteConfigKeys.ANIMAL_DETAILS_JSON),
        )
    }


    fun getAdsConfig(): AdsConfigData = adsConfigData
    fun getAssetsConfig(): AssetsConfigData = assetsConfigData
    fun getLangNativeMedia() = showLangNativeMedia
    fun getOnBoardingNativeMedia() = showOnboardingNativeMedia
    fun getAnimalNativeMedia() = showAnimalNativeMedia
    fun getNotificationTime() = notificationTime
    fun getDisableAds() = disableAds
    fun getAnimalSoundSplashAd(): Int = animalSoundSplashAd


}