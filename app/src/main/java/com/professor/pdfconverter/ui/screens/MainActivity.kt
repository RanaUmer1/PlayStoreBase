package com.professor.pdfconverter.ui.screens

import android.Manifest
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.pdfconverter.R
import com.professor.pdfconverter.databinding.ActivityMainBinding
import com.professor.pdfconverter.databinding.DialogExitBinding
import com.professor.pdfconverter.Constants
import com.professor.pdfconverter.app.AdIds
import com.professor.pdfconverter.app.AnalyticsManager
import com.professor.pdfconverter.app.AppPreferences
import com.professor.pdfconverter.model.FileType
import com.professor.pdfconverter.model.RecentFileModel
import com.professor.pdfconverter.ui.viewmodel.MainViewModel
import com.professor.pdfconverter.utils.AudioPlayerManager
import com.professor.pdfconverter.utils.NetworkChangeReceiver
import com.professor.pdfconverter.utils.Utils
import com.professor.pdfconverter.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.graphics.drawable.toDrawable
import com.professor.pdfconverter.adapter.RecentFilesAdapter

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


    private val homeIcons = Pair(R.drawable.ic_home_filled, R.drawable.ic_home)
    private val convertedIcons = Pair(R.drawable.ic_converted_filled, R.drawable.ic_converted)
    private val settingIcons = Pair(R.drawable.ic_setting_filled, R.drawable.ic_setting)


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
        setupAdapter()
        setActiveTab(binding.tvHome)
    }

    private fun setupAdapter() {
        val adapter = RecentFilesAdapter(
            onItemClick = { file ->
                // Handle file click
            },
            onMoreClick = { file ->
                // Handle more click
            }
        )
        binding.rvRecentFiles.adapter = adapter
        binding.rvRecentFiles.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        adapter.submitList(getDummyRecentFiles())
    }


    fun setActiveTab(activeTab: AppCompatTextView) {
        val tabs = listOf(binding.tvHome, binding.tvConverted, binding.tvSetting)
        val activeColor = ContextCompat.getColor(this, R.color.primary_color)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_color_secondary)

        tabs.forEach { tab ->
            val isActive = tab == activeTab
            tab.setTextColor(if (isActive) activeColor else inactiveColor)

            // Update icons based on selected state
            when (tab.id) {
                R.id.tv_home -> {
                    val iconRes = if (isActive) homeIcons.first else homeIcons.second
                    tab.setCompoundDrawablesRelativeWithIntrinsicBounds(0, iconRes, 0, 0)
                }

                R.id.tv_converted -> {
                    val iconRes = if (isActive) convertedIcons.first else convertedIcons.second
                    tab.setCompoundDrawablesRelativeWithIntrinsicBounds(0, iconRes, 0, 0)
                }

                R.id.tv_setting -> {
                    val iconRes = if (isActive) settingIcons.first else settingIcons.second
                    tab.setCompoundDrawablesRelativeWithIntrinsicBounds(0, iconRes, 0, 0)
                }
            }
        }
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


        binding.ivRemoveAd.setClickWithTimeout {
            analyticsManager.sendAnalytics(AnalyticsManager.Action.CLICKED, TAG + "_premium")
            startActivity(Intent(this, PremiumActivity::class.java))
        }


        binding.tvHome.setClickWithTimeout {
            setActiveTab(binding.tvHome)
            // Navigate to home fragment
        }

        binding.tvConverted.setClickWithTimeout {
            setActiveTab(binding.tvConverted)
            // Navigate to converted fragment
        }

        binding.tvSetting.setClickWithTimeout {
            setActiveTab(binding.tvSetting)
            // Navigate to settings fragment
        }

        /* binding.nav.btnPremium.setClickWithTimeout {
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
         }*/
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
        exitDialog.show()
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

    private fun getDummyRecentFiles(): List<RecentFileModel> {
        return listOf(
            RecentFileModel(
                id = 1L,
                name = "Sample_Document_1.pdf",
                date = "28 Nov 2025",
                time = "10:15 AM",
                size = "1.2 MB",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 2L,
                name = "Project_Proposal.docx",
                date = "27 Nov 2025",
                time = "05:42 PM",
                size = "850 KB",
                fileType = FileType.WORD
            ),
            RecentFileModel(
                id = 3L,
                name = "Invoice_2025_11.pdf",
                date = "26 Nov 2025",
                time = "09:03 AM",
                size = "560 KB",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 1L,
                name = "Sample_Document_1.pdf",
                date = "28 Nov 2025",
                time = "10:15 AM",
                size = "1.2 MB",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 2L,
                name = "Project_Proposal.docx",
                date = "27 Nov 2025",
                time = "05:42 PM",
                size = "850 KB",
                fileType = FileType.WORD
            ),
            RecentFileModel(
                id = 3L,
                name = "Invoice_2025_11.pdf",
                date = "26 Nov 2025",
                time = "09:03 AM",
                size = "560 KB",
                fileType = FileType.PDF
            ), RecentFileModel(
                id = 1L,
                name = "Sample_Document_1.pdf",
                date = "28 Nov 2025",
                time = "10:15 AM",
                size = "1.2 MB",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 2L,
                name = "Project_Proposal.docx",
                date = "27 Nov 2025",
                time = "05:42 PM",
                size = "850 KB",
                fileType = FileType.WORD
            ),
            RecentFileModel(
                id = 3L,
                name = "Invoice_2025_11.pdf",
                date = "26 Nov 2025",
                time = "09:03 AM",
                size = "560 KB",
                fileType = FileType.PDF
            )
        )
    }
}
