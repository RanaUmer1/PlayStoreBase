package com.professor.playstorebaseproject.ui.screens

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import com.professor.playstorebaseproject.utils.setClickWithTimeout

import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageBinding
    private lateinit var adapter: LanguageAdapter

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences
    lateinit var exitDialog: Dialog
    var isFromStart = false

    //    private var mSelectedLanguage = "en"

    private val TAG = "language_activity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.e(TAG, "onCreate: ")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)


        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars =
            true // Light icons = false, Dark icons = true


        isFromStart = intent.getBooleanExtra(Constants.IS_FROM_START, false)
        Log.e("TAG", "inter ad loadind lang: ${AdIds.getInterstitialWelcomeAdId()}")
//        adMobManager.interstitialAdLoader.loadAd(AdIds.getInterstitialWelcomeAdId(), null)
        loadNativeAd()
        initAdapter()
        initClickListeners()
        loadExitDialog()


//      binding.ivDone.isEnabled = adapter.selectedLanguage != null
    }

    private fun loadExitDialog() {
        exitDialog = Dialog(this)
        val binding = DialogExitBinding.inflate(layoutInflater)
        exitDialog.setContentView(binding.root)
        exitDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        loadExitBannerAd(binding)

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
    }

    private fun loadExitBannerAd(binding: DialogExitBinding) {
        adMobManager.bannerAdLoader.showMemRecBanner(
            this,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd,
            AdIds.getBannerAdIdExit()
        )
    }

    private fun loadNativeAd() {


        if (adMobManager.nativeAdLoader.isAdLoaded() && isFromStart) {
            Log.e("Monetization", "onCreate: show native ad")
            adMobManager.nativeAdLoader.showLoadedAd(
                NativeAdBuilder.Builder(
                    R.layout.native_ad_lang,
                    binding.includeAd.adFrame,
                    binding.includeAd.shimmerFbAd
                ).setShowMedia(RemoteConfigManager.getLangNativeMedia()).setShowBody(true)
                    .setIconEnabled(true)
                    .setShowRating(false)
                    .setAdTitleColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].heading)
                    .setAdBodyColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].description)
                    .setCtaTextColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].ctaText)
                    .setCtaBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].callActionButtonColor)
                    //.setAdBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].backgroundColor)
                    .build(), AdIds.getNativeLanguageAdId()
            )

            adMobManager.nativeAdLoader.loadAd(AdIds.getNativeOnboardingAdId())
            adMobManager.nativeAdLoader.loadAd(AdIds.getFullNativeOnboardingAdId())
        } else {
            Log.e("Monetization", "load native ad Language screen: ")
            adMobManager.nativeAdLoader.loadAndShow(
                AdIds.getNativeLanguageAdId(), NativeAdBuilder.Builder(
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
                    //.setAdBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].backgroundColor)
                    .build()
            ) {}


        }
    }

    private fun initClickListeners() {


        binding.ivDone.setClickWithTimeout {
            binding.ivDone.isEnabled = false // Prevent double taps

            analyticsManager.sendAnalytics(
                AnalyticsManager.Action.CLICKED, "btn_select_language_done"
            )

            val selectedLanguage = adapter.selectedLanguageModel
            if (selectedLanguage == null) {
                Toast.makeText(
                    this, getString(R.string.please_select_a_language), Toast.LENGTH_SHORT
                ).show()
//                binding.ivDone.isEnabled = true
                return@setClickWithTimeout
            }

            val savedLanguageId = appPreferences.getInt(AppPreferences.LANGUAGE_ID)
            if (selectedLanguage.id != savedLanguageId) {
                appPreferences.setInt(AppPreferences.LANGUAGE_ID, selectedLanguage.id)
                appPreferences.setString(AppPreferences.LANGUAGE_CODE, selectedLanguage.code)
            }

            navigate()
        }
    }

    private fun navigate() {

        if (!appPreferences.getBoolean(AppPreferences.IS_LANGUAGE_SELECTED)) {
            appPreferences.setBoolean(AppPreferences.IS_LANGUAGE_SELECTED, true)
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        } else {
            startActivity(
                Intent(
                    this, MainActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

    }

    private fun onLanguageSelected(selectedLang: LanguageModel) {
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

//    private fun initAdapter() {
//        val savedLanguageId = appPreferences.getInt(AppPreferences.LANGUAGE_ID)
//        val selectedLanguage = getLanguageList(this).find { it.id == savedLanguageId }
//        analyticsManager.sendAnalytics("selected", "${selectedLanguage?.name}")
//        adapter = LanguageAdapter(selectedLanguage) {
//            onLanguageSelected(it)
//        }
//
//        adapter.submitList(getLanguageList(this))
//        binding.rvLanguage.layoutManager = LinearLayoutManager(this)
//        binding.rvLanguage.adapter = adapter
//    }


    private fun initAdapter() {
        val languageList = getLanguageList(this)
        val savedLanguageId = appPreferences.getInt(AppPreferences.LANGUAGE_ID)
        val savedLanguage = languageList.find { it.id == savedLanguageId }

        val deviceLangCode = Locale.getDefault().language
        val defaultLanguage = savedLanguage ?: languageList.find { it.code == deviceLangCode }  ?: languageList.find { it.code == "en" } // fallback to English


        val displayList = mutableListOf<LanguageListItem>()

        defaultLanguage?.let {
            displayList.add(LanguageListItem.Header("Default"))
            displayList.add(LanguageListItem.Language(it))
        }

        displayList.add(LanguageListItem.Header("All Languages"))
        languageList.filterNot { it == defaultLanguage }
            .forEach { displayList.add(LanguageListItem.Language(it)) }

        adapter = LanguageAdapter(defaultLanguage) {
            onLanguageSelected(it.model)
        }

        binding.rvLanguage.layoutManager = LinearLayoutManager(this)
        binding.rvLanguage.adapter = adapter
        adapter.submitList(displayList)

        onLanguageSelected(defaultLanguage ?: languageList.first())
    }


    private fun getLanguageList(activity: Activity): List<LanguageModel> {
        val languageModelList: MutableList<LanguageModel> = ArrayList()
        languageModelList.add(
            LanguageModel(
                1,
                R.drawable.flag_arabic,
                activity.resources.getString(R.string.arabic),
                "ar",

                )
        )
        languageModelList.add(
            LanguageModel(
                2,
                R.drawable.flag_english,
                activity.resources.getString(R.string.english),
                "en",

                )
        )

        languageModelList.add(
            LanguageModel(
                3,
                R.drawable.flag_spanish,
                activity.resources.getString(R.string.spanish),
                "es",

                )
        )


        languageModelList.add(
            LanguageModel(
                4,
                R.drawable.flag_indonesia,
                activity.resources.getString(R.string.indonesian),
                "in",

                )
        )
        languageModelList.add(
            LanguageModel(
                6,
                R.drawable.flag_persian,
                activity.resources.getString(R.string.persian),
                "fa",

                )
        )
        languageModelList.add(
            LanguageModel(
                7,
                R.drawable.flag_hindi,
                activity.resources.getString(R.string.hindi),
                "hi",

                )
        )

        languageModelList.add(
            LanguageModel(
                8,
                R.drawable.flag_russia,
                activity.resources.getString(R.string.russian),
                "ru",

                )
        )
        languageModelList.add(
            LanguageModel(
                9,
                R.drawable.flag_portuguese,
                activity.resources.getString(R.string.portuguese),
                "pt",

                )
        )
        languageModelList.add(
            LanguageModel(
                10,
                R.drawable.flag_bangla,
                activity.resources.getString(R.string.bengali),
                "bn",

                )
        )
        languageModelList.add(
            LanguageModel(
                11,
                R.drawable.flag_turkey,
                activity.resources.getString(R.string.turkish),
                "tr",

                )
        )

        return languageModelList
    }

    override fun onBackPressed() {
        if (isFromStart) {
            exitDialog.show()
        } else {
            finish()
        }
    }

}
