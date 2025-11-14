package com.professor.playstorebaseproject.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.mzalogics.ads.domain.ads.native_ad.NativeAdBuilder
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.playstorebaseproject.R
import com.professor.playstorebaseproject.databinding.ActivityOnboardingBinding
import com.professor.playstorebaseproject.Constants
import com.professor.playstorebaseproject.adapter.OnboardingAdapter
import com.professor.playstorebaseproject.app.AdIds
import com.professor.playstorebaseproject.app.AnalyticsManager
import com.professor.playstorebaseproject.app.AppPreferences
import com.professor.playstorebaseproject.model.OnboardingItem
import com.professor.playstorebaseproject.remoteconfig.RemoteConfigManager
import com.professor.playstorebaseproject.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingAdapter

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences
    private var mLastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        val localeList =
            LocaleListCompat.forLanguageTags(appPreferences.getString(AppPreferences.LANGUAGE_CODE))
        AppCompatDelegate.setApplicationLocales(localeList)
        setContentView(binding.root)



        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars =
            true // Light icons = false, Dark icons = true

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "activity_onboarding")

        val onboardingItems = listOf(
            OnboardingItem(
                title = getString(R.string.onboarding_title_1),
                description = getString(R.string.onboarding_desc_1),
                imageRes = R.drawable.onboarding_1
            ),
            OnboardingItem(
                title = getString(R.string.onboarding_title_2),
                description = getString(R.string.onboarding_desc_2),
                imageRes = R.drawable.onboarding_2
            )
        )

        adapter = OnboardingAdapter(onboardingItems,adMobManager)
        binding.viewPager.adapter = adapter

        setupDotsIndicator(adapter.itemCount, binding.viewPager)


        binding.btnNext.setClickWithTimeout {
            if (binding.viewPager.currentItem < onboardingItems.size - 1) {
                binding.viewPager.currentItem += 1
            } else {
                moveToMain()
            }
        }


        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtonText(position, binding.viewPager)
            }
        })


        loadNativeAd()



    }


    private fun updateButtonText(position: Int, viewPager: ViewPager2) {


        when (position) {
            0 -> {
                binding.btnNext.text = getString(R.string.next)
                binding.llIndicators.isVisible = true
                binding.includeAd.adRoot.isVisible = false
            }

            1 -> {
                binding.llIndicators.isVisible = false
                binding.includeAd.adRoot.isVisible = false
                binding.btnNext.text = getString(R.string.next)
            }

            2 -> {
                binding.llIndicators.isVisible = true
                binding.includeAd.adRoot.isVisible = true
                binding.btnNext.text = getString(R.string.start)
            }
        }

    }

    private fun setupDotsIndicator(pageCount: Int, viewPager: ViewPager2) {
        val dotContainer = binding.dotContainer
        val dots = mutableListOf<TextView>()

        for (i in 0 until pageCount) {
            val dot = TextView(this).apply {
                text = "•"
                textSize = 32f
                setTextColor(if (i == 0) getColor(R.color.accent_color) else getColor(R.color.text_color_gray))
                setPadding(2, 0, 2, 0)
            }
            dots.add(dot)
            dotContainer.addView(dot)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                for (i in dots.indices) {
                    dots[i].setTextColor(
                        if (i == position) getColor(R.color.accent_color) else getColor(
                            R.color.text_color_gray
                        )
                    )
                }
            }
        })
    }

    private fun loadNativeAd() {
        if (adMobManager.nativeAdLoader.isAdLoaded()) {
            Log.e("TAG", "show loaded: ")
            adMobManager.nativeAdLoader.showLoadedAd(
                NativeAdBuilder.Builder(
                    R.layout.native_ad_onboarding,
                    binding.includeAd.adFrame,
                    binding.includeAd.shimmerFbAd
                ).setShowBody(true)
                    .setShowMedia(RemoteConfigManager.getOnBoardingNativeMedia())
                    .setAdTitleColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].heading)
                    .setAdBodyColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].description)
                    .setCtaTextColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].ctaText)
                    .setCtaBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].callActionButtonColor)
//                    .setAdBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].backgroundColor)
                    .build(), AdIds.getNativeOnboardingAdId()
            )
        } else {
            Log.e("TAG", "load and show: ")
            adMobManager.nativeAdLoader.loadAndShow(
                AdIds.getNativeOnboardingAdId(),
                NativeAdBuilder.Builder(
                    R.layout.native_ad_onboarding,
                    binding.includeAd.adFrame,
                    binding.includeAd.shimmerFbAd
                ).setShowBody(true)
                    .setShowMedia(RemoteConfigManager.getOnBoardingNativeMedia())
                    .setAdTitleColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].heading)
                    .setAdBodyColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].description)
                    .setCtaTextColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].ctaText)
                    .setCtaBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].callActionButtonColor)
//                    .setAdBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].backgroundColor)
                    .build()
            ) {}

        }
    }


    private fun moveToMain() {

        appPreferences.setBoolean(AppPreferences.IS_ONBOARDING, true)

        /**
         * 0 -> MainActivity
         * 1 -> PremiumActivity -> InterstitialAd -> MainActivity
         * 2 -> InterstitialAd -> MainActivity
         */


        when (RemoteConfigManager.getAdsConfig().onBoardingMonetizationStrategy) {
            0 -> {
                val intent = Intent(this@OnboardingActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

            1 -> {
                val intent = Intent(this@OnboardingActivity, PremiumActivity::class.java)
                intent.putExtra(Constants.IS_FROM_START_TO_PREMIUM, true)
                startActivity(intent)
                finish()
            }

            2 -> {
                adMobManager.interstitialAdLoader.showAd(
                    this,
                    AdIds.getInterstitialAdID()
                ) {
                    Log.e("TAG", "moveToMain: ---")
                    val intent = Intent(this@OnboardingActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }

            else -> {
                val intent = Intent(this@OnboardingActivity, PremiumActivity::class.java)
                intent.putExtra(Constants.FROM_ONBOARDING, true)
                startActivity(intent)
                finish()
            }
        }

    }
}