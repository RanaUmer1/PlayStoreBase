package com.professor.playstorebaseproject.ui.screens

/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com
 */

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.mzalogics.ads.domain.consent.AdsConsentManager
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.playstorebaseproject.Constants
import com.professor.playstorebaseproject.app.AdIds
import com.professor.playstorebaseproject.app.AnalyticsManager
import com.professor.playstorebaseproject.app.AppPreferences
import com.professor.playstorebaseproject.data.repository.DataRepository
import com.professor.playstorebaseproject.databinding.ActivityStartBinding
import com.professor.playstorebaseproject.remoteconfig.RemoteConfigManager
import com.professor.playstorebaseproject.utils.mainCoroutine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class StartActivity : AppCompatActivity() {

    private val TAG = StartActivity::class.java.simpleName

    private lateinit var binding: ActivityStartBinding
    private var job: Job? = null

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var dataRepository: DataRepository

    private val adsConsentManager by lazy { AdsConsentManager(this) }


    private var isPremium: Boolean = false


    private val isLanguageSelected: Boolean
        get() = appPreferences.getBoolean(AppPreferences.IS_LANGUAGE_SELECTED, false)

    private val isOnboarding: Boolean
        get() = appPreferences.getBoolean(AppPreferences.IS_ONBOARDING, false)


    private var hasMovedToNext = false
    private var isAdShow = false

    private val splashDelayLength = 8000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars =
            true // Light icons = false, Dark icons = true


        // Preload somewhere sensible (e.g., onResume)


        //hideSystemUI()
        adMobManager = AdMobManager.getInstance(application)
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)
        initUI()
    }

    override fun onStart() {
        super.onStart()
        if (isAdShow) {
            moveToNextScreen()
            isAdShow = false
        }

    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    private fun initUI() {
        isPremium = appPreferences.getBoolean(AppPreferences.IS_PREMIUM)
        RemoteConfigManager.fetchRemoteConfig { success ->
            Log.d(TAG, "RemoteConfigs initialized: $success")
            if (RemoteConfigManager.getDisableAds()) {
                appPreferences.setBoolean(AppPreferences.IS_PREMIUM, true)
                isPremium = appPreferences.getBoolean(AppPreferences.IS_PREMIUM)
                AdMobManager.isPremium = isPremium
            } else {
                appPreferences.setBoolean(AppPreferences.IS_PREMIUM, false)
                isPremium = appPreferences.getBoolean(AppPreferences.IS_PREMIUM)
                AdMobManager.isPremium = isPremium
            }

            if (!isPremium) initConsent() else moveToNextScreen()
        }
    }

    private fun initConsent() {
        if (!adsConsentManager.canRequestAds) {
            adsConsentManager.showGDPRConsent(this, false) { error ->
                error?.let {
                    Log.w(TAG, "Consent error: ${it.errorCode} - ${it.message}")
                }
                setMediationConsent(adsConsentManager.canRequestAds)
                proceedWithAds()
            }
        } else {
            proceedWithAds()
        }
    }

    private fun setMediationConsent(isConsent: Boolean) {
        Log.e(TAG, "setMediationConsent: ")
//        AppLovinPrivacySettings.setHasUserConsent(isConsent)
//        AppLovinPrivacySettings.setDoNotSell(isConsent)
//        VunglePrivacySettings.setGDPRStatus(isConsent, "v1.0.0")
//        VunglePrivacySettings.setCCPAStatus(isConsent)

    }

    private fun proceedWithAds() {

        Log.d(TAG, "Initializing Ads")

        adMobManager.setAppOpenAdResumeId(AdIds.getAppOpenAdIdResume())
            .setAppOpenAdStartId(AdIds.getAppOpenAdId()).setShouldShowResumeAd(true)
            .setInterstitialAdMaxTime(
                RemoteConfigManager.getAdsConfig().interstitialMaxTimer?.toLong() ?: 20
            ).setInterstitialAdMinTime(
                RemoteConfigManager.getAdsConfig().interstitialMinTimer?.toLong() ?: 10
            ).setInterstitialCounter(RemoteConfigManager.getAdsConfig().interstitialCounter ?: 2)
            .setOpenAdResumeTime(
                RemoteConfigManager.getAdsConfig().openAdResumeTimer?.toLong() ?: 10
            ).setPremium(false).setSplash(true)



        job = mainCoroutine {
            delay(splashDelayLength)
            Log.e(TAG, "splash delay: ")
            moveToNextScreen()
        }
        Log.e(TAG, "proceedWithAds----: ${!isLanguageSelected}")


        if (!isLanguageSelected) {
            adMobManager.nativeAdLoader.loadAd(AdIds.getNativeLanguageAdId())
        }
        adMobManager.interstitialAdLoader.loadAd(AdIds.getInterstitialSplashAdId()) { isAdLoaded ->
            lifecycleScope.launch {
                if (isAdLoaded) {
                    Log.e(TAG, "inter ad is loaded: ")
                    job?.cancel()
                    job = null
                    if (!hasMovedToNext) {
                        Log.e(TAG, "proceedWithAds: show ad")
                        isAdShow = true
                        adMobManager.interstitialAdLoader.showAd(
                            this@StartActivity, AdIds.getInterstitialSplashAdId()
                        ) {}

                    } else
                        moveToNextScreen()

                } else
                    Log.e(TAG, "now show inter else case: ")
            }
        }


//        adMobManager.appOpenAdLoader.loadAppOpenAd(this) { adShown ->
//            if (adShown) {
//                job?.cancel()
//                adMobManager.appOpenAdLoader.showAppOpenAdIfAvailable {
//                    Log.d(TAG, "App Open Ad finished: $it")
//                    moveToNextScreen()
//                }
//            } else {
//                moveToNextScreen()
//            }
//        }

    }

    private fun moveToNextScreen() {
        Log.e(TAG, "moveToNextScreen: ")
        if (hasMovedToNext) return
        hasMovedToNext = true
        job?.cancel()
        job = null

        val nextActivity = when {
            !isLanguageSelected -> {
                Intent(this, LanguageActivity::class.java).apply {
                    putExtra(Constants.IS_FROM_START, true)
                }
            }

            !isOnboarding -> {
                Intent(this, OnboardingActivity::class.java)
            }


            else -> {
                Intent(
                    this,
                    PremiumActivity::class.java
                ).putExtra(Constants.IS_FROM_START_TO_PREMIUM, true)
            }

        }
        startActivity(nextActivity)
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        adMobManager.setSplash(false)
        job?.cancel()
    }
}
