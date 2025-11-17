package com.professor.playstorebaseproject.ui.screens

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mzalogics.ads.domain.ads.native_ad.NativeAdBuilder
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.playstorebaseproject.R
import com.professor.playstorebaseproject.databinding.ActivityLanguageBinding
import com.professor.playstorebaseproject.databinding.DialogExitBinding
import com.professor.playstorebaseproject.Constants
import com.professor.playstorebaseproject.adapter.LanguageAdapter
import com.professor.playstorebaseproject.app.AdIds
import com.professor.playstorebaseproject.app.AnalyticsManager
import com.professor.playstorebaseproject.app.AppPreferences
import com.professor.playstorebaseproject.model.LanguageListItem
import com.professor.playstorebaseproject.model.LanguageModel
import com.professor.playstorebaseproject.remoteconfig.RemoteConfigManager
import com.professor.playstorebaseproject.ui.screens.language.LanguageViewModel
import com.professor.playstorebaseproject.ui.viewmodel.AdState
import com.professor.playstorebaseproject.ui.viewmodel.LanguageViewModel
import com.professor.playstorebaseproject.utils.UIState
import com.professor.playstorebaseproject.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageBinding
    private lateinit var adapter: LanguageAdapter
    private val viewModel: LanguageViewModel by viewModels()

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    private lateinit var exitDialog: Dialog
    private var isFromStart = false
    private var nativeAdLoadAttempted = false

    private val TAG = "LanguageActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate: LanguageActivity started")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)

        setupWindowInsets()
        initializeActivity()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Native ad state = ${viewModel.adState.value}")

        // Try to show ad if it wasn't shown yet
        if (viewModel.adState.value == AdState.LOADED) {
            showNativeAd()
        } else if (viewModel.adState.value == AdState.FAILED && !nativeAdLoadAttempted) {
            loadNativeAdWithRetry()
        }
    }

    private fun setupWindowInsets() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
    }

    private fun initializeActivity() {
        isFromStart = intent.getBooleanExtra(Constants.IS_FROM_START, false)
        Log.d(TAG, "isFromStart: $isFromStart")

        initAdapter()
        initClickListeners()
        loadExitDialog()

        // Preload onboarding ads immediately
        preloadOnboardingAds()

        // Handle native ad based on preloading from StartActivity
        handleNativeAd()
    }

    private fun setupObservers() {
        // Observe UI state
        viewModel.uiState.collect { state ->
            when (state) {
                is UIState.Loading -> {
                    Log.d(TAG, "Loading language data...")
                    // Show loading state if needed
                }
                is UIState.Success -> {
                    Log.d(TAG, "Language data loaded successfully")
                    val uiState = state.data
                    adapter.submitList(uiState.displayList)
                    updateDoneButton(uiState.selectedLanguage)
                }
                is UIState.Error -> {
                    Log.e(TAG, "Error loading language data: ${state.throwable.message}")
                    Toast.makeText(this, "Error loading languages", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observe selected language
        viewModel.selectedLanguage.observe(this) { language ->
            language?.let {
                updateDoneButton(it)
                binding.ivDone.isEnabled = true
            }
        }

        // Observe ad state
        viewModel.adState.observe(this) { state ->
            Log.d(TAG, "Ad state changed: $state")
            when (state) {
                AdState.FAILED -> {
                    showAdLoadingFailedUI()
                }
                AdState.SHOWING -> {
                    analyticsManager.sendAnalytics("ad_show_success", "language_native")
                }
                else -> {
                    // Handle other states if needed
                }
            }
        }
    }

    private fun preloadOnboardingAds() {
        Log.d(TAG, "Preloading onboarding ads...")

        adMobManager.nativeAdLoader.loadAd(AdIds.getNativeOnboardingAdId()) { ]
            if (isLoaded) {
                Log.d(TAG, "Onboarding native ad preloaded successfully")
                analyticsManager.sendAnalytics("ad_preload_success", "onboarding_native")
            } else {
                Log.w(TAG, "Failed to preload onboarding native ad")
                analyticsManager.sendAnalytics("ad_preload_failed", "onboarding_native")
            }
        }

        adMobManager.nativeAdLoader.loadAd(AdIds.getFullNativeOnboardingAdId()) { isLoaded ->
            if (isLoaded) {
                Log.d(TAG, "Full onboarding native ad preloaded successfully")
                analyticsManager.sendAnalytics("ad_preload_success", "full_onboarding_native")
            } else {
                Log.w(TAG, "Failed to preload full onboarding native ad")
                analyticsManager.sendAnalytics("ad_preload_failed", "full_onboarding_native")
            }
        }
    }

    private fun handleNativeAd() {
        Log.d(TAG, "Handling native ad, state from StartActivity: ${adMobManager.nativeAdLoader.isAdLoaded()}")

        if (adMobManager.nativeAdLoader.isAdLoaded() && isFromStart) {
            Log.d(TAG, "Showing preloaded native ad from StartActivity")
            analyticsManager.sendAnalytics("ad_show_preloaded", "language_native")
            showNativeAd()
        } else {
            Log.d(TAG, "Loading native ad for LanguageActivity")
            loadNativeAdWithRetry()
        }
    }

    private fun loadNativeAdWithRetry(maxRetries: Int = 2) {
        if (nativeAdLoadAttempted) {
            Log.d(TAG, "Native ad load already attempted, skipping retry")
            return
        }

        nativeAdLoadAttempted = true
        viewModel.updateAdState(AdState.LOADING)

        Log.d(TAG, "Loading native ad for Language screen (attempt 1)")

        adMobManager.nativeAdLoader.loadAndShow(
            AdIds.getNativeLanguageAdId(),
            createNativeAdBuilder(),
            onAdLoaded = { isLoaded ->
                if (isLoaded) {
                    Log.d(TAG, "Native ad loaded and shown successfully")
                    viewModel.updateAdState(AdState.SHOWING)
                    analyticsManager.sendAnalytics("ad_show_success", "language_native_loaded")
                } else {
                    Log.w(TAG, "Native ad failed to load on first attempt")
                    viewModel.updateAdState(AdState.FAILED)
                    analyticsManager.sendAnalytics("ad_show_failed", "language_native_first_attempt")

                    // Retry after delay
                    if (maxRetries > 0) {
                        binding.root.postDelayed({
                            retryNativeAdLoad(maxRetries - 1)
                        }, 1000L)
                    }
                }
            }
        )
    }

    private fun retryNativeAdLoad(retryCount: Int) {
        Log.d(TAG, "Retrying native ad load, attempts left: $retryCount")
        viewModel.updateAdState(AdState.RETRYING)

        adMobManager.nativeAdLoader.loadAndShow(
            AdIds.getNativeLanguageAdId(),
            createNativeAdBuilder(),
            onAdLoaded = { isLoaded ->
                if (isLoaded) {
                    Log.d(TAG, "Native ad loaded successfully on retry $retryCount")
                    viewModel.updateAdState(AdState.SHOWING)
                    analyticsManager.sendAnalytics("ad_show_success", "language_native_retry_$retryCount")
                } else if (retryCount > 0) {
                    Log.w(TAG, "Native ad failed on retry $retryCount")
                    analyticsManager.sendAnalytics("ad_show_failed", "language_native_retry_$retryCount")

                    binding.root.postDelayed({
                        if (retryCount == 1) {
                            loadFallbackNativeAd()
                        }
                    }, 1500L)
                }
            }
        )
    }

    private fun loadFallbackNativeAd() {
        Log.d(TAG, "Loading fallback native ad")

        adMobManager.nativeAdLoader.loadAndShow(
            AdIds.getNativeLanguageAdId(),
            createNativeAdBuilder(),
            onAdLoaded = { isLoaded ->
                if (isLoaded) {
                    Log.d(TAG, "Fallback native ad loaded successfully")
                    viewModel.updateAdState(AdState.SHOWING)
                    analyticsManager.sendAnalytics("ad_show_success", "language_native_fallback")
                } else {
                    Log.e(TAG, "All native ad loading attempts failed")
                    viewModel.updateAdState(AdState.FAILED)
                    analyticsManager.sendAnalytics("ad_show_failed", "language_native_all_attempts")
                    showAdLoadingFailedUI()
                }
            }
        )
    }

    private fun showNativeAd() {
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "Activity finishing, skipping ad show")
            return
        }

        Log.d(TAG, "Showing preloaded native ad")
        viewModel.updateAdState(AdState.SHOWING)

        val success = adMobManager.nativeAdLoader.showLoadedAd(
            createNativeAdBuilder().build(),
            AdIds.getNativeLanguageAdId()
        )

        if (success) {
            Log.d(TAG, "Preloaded native ad shown successfully")
            analyticsManager.sendAnalytics("ad_show_success", "language_native_preloaded")
            preloadOnboardingAds()
        } else {
            Log.w(TAG, "Failed to show preloaded native ad, loading fresh")
            viewModel.updateAdState(AdState.FAILED)
            loadNativeAdWithRetry()
        }
    }

    private fun createNativeAdBuilder(): NativeAdBuilder.Builder {
        return NativeAdBuilder.Builder(
            R.layout.native_ad_lang,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd
        ).setShowMedia(RemoteConfigManager.getLangNativeMedia())
            .setShowBody(true)
            .setIconEnabled(true)
            .setShowRating(false)
            .setAdTitleColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].heading)
            .setAdBodyColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].description)
            .setCtaTextColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].ctaText)
            .setCtaBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].callActionButtonColor)
            .build()
    }

    private fun showAdLoadingFailedUI() {
        binding.includeAd.adFrame.visibility = android.view.View.GONE
        binding.includeAd.shimmerFbAd.visibility = android.view.View.GONE
    }

    private fun loadExitDialog() {
        exitDialog = Dialog(this)
        val binding = DialogExitBinding.inflate(layoutInflater)
        exitDialog.setContentView(binding.root)
        exitDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val layoutParams = binding.root.layoutParams
        val width = resources.displayMetrics.widthPixels
        layoutParams.width = (width * 0.9).toInt()
        binding.root.layoutParams = layoutParams

        exitDialog.setCancelable(true)
        binding.btnNo.setClickWithTimeout { exitDialog.dismiss() }
        binding.btnYes.setClickWithTimeout {
            exitDialog.dismiss()
            finishAffinity()
        }

        loadExitBannerAd(binding)
    }

    private fun loadExitBannerAd(binding: DialogExitBinding) {
        adMobManager.bannerAdLoader.showMemRecBanner(
            this,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd,
            AdIds.getBannerAdIdExit()
        ) { isLoaded ->
            if (isLoaded) {
                Log.d(TAG, "Exit banner ad loaded successfully")
                analyticsManager.sendAnalytics("ad_show_success", "exit_banner")
            } else {
                Log.w(TAG, "Exit banner ad failed to load")
                analyticsManager.sendAnalytics("ad_show_failed", "exit_banner")
            }
        }
    }

    private fun initClickListeners() {
        binding.ivDone.setClickWithTimeout {
            binding.ivDone.isEnabled = false

            analyticsManager.sendAnalytics(
                AnalyticsManager.Action.CLICKED, "btn_select_language_done"
            )

            if (!viewModel.isLanguageSelected()) {
                Toast.makeText(
                    this, getString(R.string.please_select_a_language), Toast.LENGTH_SHORT
                ).show()
                binding.ivDone.isEnabled = true
                return@setClickWithTimeout
            }

            viewModel.selectedLanguage.value?.let { selectedLanguage ->
                analyticsManager.sendAnalytics("language_selected", "${selectedLanguage.name} (${selectedLanguage.code})")
            }

            viewModel.saveSelectedLanguage()
            navigateToNextScreen()
        }
    }

    private fun initAdapter() {
        adapter = LanguageAdapter(null) { languageItem ->
            viewModel.selectLanguage(languageItem.model)
        }

        binding.rvLanguage.layoutManager = LinearLayoutManager(this)
        binding.rvLanguage.adapter = adapter
    }

    private fun updateDoneButton(selectedLang: LanguageModel) {
        binding.ivDone.isEnabled = true
        binding.btnDone.text = getString(
            when (selectedLang.id) {
                1 -> R.string.done_in_arabic
                2 -> R.string.done_in_english
                3 -> R.string.done_in_spanish
                4 -> R.string.done_in_indonesian
                5 -> R.string.done_in_french
                6 -> R.string.done_in_persian
                7 -> R.string.done_in_hindi
                8 -> R.string.done_in_russian
                9 -> R.string.done_in_portuguese
                10 -> R.string.done_in_bengali
                11 -> R.string.done_in_turkish
                else -> R.string.done_in_english
            }
        )
    }

    private fun navigateToNextScreen() {
        Log.d(TAG, "Navigating to next screen")

        if (viewModel.shouldShowOnboarding()) {
            analyticsManager.sendAnalytics("navigation", "language_to_onboarding")
            startActivity(Intent(this, OnboardingActivity::class.java))
        } else {
            analyticsManager.sendAnalytics("navigation", "language_to_main")
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        finish()
    }

    override fun onBackPressed() {
        if (isFromStart) {
            exitDialog.show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Final ad state = ${viewModel.adState.value}")
        analyticsManager.sendAnalytics("activity_destroyed", "language_ad_state_${viewModel.adState.value}")
    }
}