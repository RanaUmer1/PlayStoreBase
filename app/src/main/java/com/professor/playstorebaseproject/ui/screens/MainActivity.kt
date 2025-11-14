package com.professor.playstorebaseproject.ui.screens

import android.Manifest
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.playstorebaseproject.R
import com.professor.playstorebaseproject.databinding.ActivityMainBinding
import com.professor.playstorebaseproject.databinding.DialogExitBinding
import com.professor.playstorebaseproject.Constants
import com.professor.playstorebaseproject.app.AdIds
import com.professor.playstorebaseproject.app.AnalyticsManager
import com.professor.playstorebaseproject.app.AppPreferences
import com.professor.playstorebaseproject.ui.viewmodel.MainViewModel
import com.professor.playstorebaseproject.utils.AudioPlayerManager
import com.professor.playstorebaseproject.utils.NetworkChangeReceiver
import com.professor.playstorebaseproject.utils.Utils
import com.professor.playstorebaseproject.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import androidx.core.graphics.drawable.toDrawable

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    private val viewModel: MainViewModel by viewModels()
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private lateinit var binding: ActivityMainBinding
    private lateinit var exitDialog: Dialog
    private val notificationPermissionBottomSheet = NotificationPermissionBottomSheet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(appPreferences.getString(AppPreferences.LANGUAGE_CODE))
        )
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)

        loadAd()
        init()
        listeners()
        loadExitDialog()
    }

    override fun onResume() {
        super.onResume()
        networkChangeReceiver = NetworkChangeReceiver(this)
        registerReceiver(
            networkChangeReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    override fun onPause() {
        super.onPause()
        AudioPlayerManager.shouldPlay = false
        AudioPlayerManager.stop()
        unregisterReceiver(networkChangeReceiver)
    }

    override fun onDestroy() {
        AudioPlayerManager.shouldPlay = false
        super.onDestroy()
    }

    private fun init() {
        requestPermission()
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionBottomSheet.show(
                supportFragmentManager,
                "NotificationPermissionBottomSheet"
            )
        }
    }


    private fun listeners() {


        binding.ivDrawer.setClickWithTimeout {
            binding.drawerLayout.openDrawer(GravityCompat.START)
            analyticsManager.sendAnalytics(AnalyticsManager.Action.CLICKED, TAG + "_drawer")
        }

        binding.ivRemoveAd.setClickWithTimeout {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.CLICKED, TAG + "_premium")
            startActivity(Intent(this, PremiumActivity::class.java))
        }


        binding.nav.btnPremium.setClickWithTimeout {
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        binding.nav.tvLanguage.setClickWithTimeout {
            analyticsManager.sendAnalytics(
                AnalyticsManager.Action.CLICKED,
                TAG + "_drawer_language"
            )
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(
                Intent(this, LanguageActivity::class.java)
                    .putExtra(Constants.IS_FROM_START, false)
            )
        }



        binding.nav.tvResetRingtone.setClickWithTimeout {
            analyticsManager.sendAnalytics(
                AnalyticsManager.Action.CLICKED,
                TAG + "_drawer_reset_ringtone"
            )
            binding.drawerLayout.closeDrawer(GravityCompat.START)

            if (!Settings.System.canWrite(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
                Utils.resetDefaultRingtones(this)
            }
        }

        binding.nav.tvFeedback.setClickWithTimeout {
            analyticsManager.sendAnalytics(
                AnalyticsManager.Action.CLICKED,
                TAG + "_drawer_feedback"
            )
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            openStorePage()
        }

        binding.nav.tvRateUs.setClickWithTimeout {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.CLICKED, TAG + "_drawer_rate_us")
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            openStorePage()
        }

        binding.nav.tvPrivacy.setClickWithTimeout {
            analyticsManager.sendAnalytics(
                AnalyticsManager.Action.CLICKED,
                TAG + "_drawer_privacy_policy"
            )
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://ozi-apps.s3.us-west-2.amazonaws.com/RealAnimalSound_PrivacyPolicy.html")
                )
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
            )
        }

        binding.nav.tvShare.setClickWithTimeout {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.CLICKED, TAG + "_drawer_share")
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            shareApp()
        }
    }

    private fun loadAd() {

        adMobManager.bannerAdLoader.showCollapsableBanner(
            this,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd,
            AdIds.getBannerHomeAdId(), false
        )
    }

    private fun loadExitBannerAd(binding: DialogExitBinding) {
        adMobManager.bannerAdLoader.showMemRecBanner(
            this,
            binding.includeAd.adFrame,
            binding.includeAd.shimmerFbAd,
            AdIds.getBannerAdIdExit()
        )
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, getString(R.string.shareApp))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name))
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }

    private fun backPressed() {
        when {
            binding.drawerLayout.isOpen -> binding.drawerLayout.closeDrawer(GravityCompat.START)
            else -> exitDialog.show()
        }
    }


    private fun loadExitDialog() {
        exitDialog = Dialog(this).apply {
            val exitBinding = DialogExitBinding.inflate(layoutInflater)
            setContentView(exitBinding.root)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            loadExitBannerAd(exitBinding)
            exitBinding.root.layoutParams.width =
                (resources.displayMetrics.widthPixels * 0.9).toInt()
            setCancelable(true)
            exitBinding.btnNo.setClickWithTimeout { dismiss() }
            exitBinding.btnYes.setClickWithTimeout {
                dismiss()
                finishAffinity()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Notification permission granted
        }
    }

    override fun onBackPressed() {
        analyticsManager.sendAnalytics("clicked", "$TAG _back_btn")
        backPressed()
    }


    private fun openStorePage() {
        val url = "https://play.google.com/store/apps/details?id=$packageName"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
    }

    companion object {
        const val NOTIFICATION_PERMISSION_CODE = 1001
    }
}
