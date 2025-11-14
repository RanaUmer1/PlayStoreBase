package com.professor.playstorebaseproject.ui.screens


import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.playstorebaseproject.R
import com.professor.playstorebaseproject.databinding.PremiumActivtyNewBinding
import com.professor.playstorebaseproject.Constants
import com.professor.playstorebaseproject.app.AdIds
import com.professor.playstorebaseproject.app.AnalyticsManager
import com.professor.playstorebaseproject.app.AppPreferences
import com.professor.playstorebaseproject.iab.AppBillingClient
import com.professor.playstorebaseproject.iab.ProductItem
import com.professor.playstorebaseproject.iab.interfaces.ConnectResponse
import com.professor.playstorebaseproject.iab.interfaces.PurchaseResponse
import com.professor.playstorebaseproject.iab.subscription.SubscriptionItem
import com.professor.playstorebaseproject.remoteconfig.RemoteConfigManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class PremiumActivity : AppCompatActivity(), View.OnClickListener {

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences


    private var appBillingClient: AppBillingClient? = null
    private lateinit var binding: PremiumActivtyNewBinding


    var skuDetail: List<SubscriptionItem>? = null
    var fromPro: Boolean = false

    var isWeekly: Boolean = true
    var hasTrial: Boolean = false

    //    MakePurchaseViewModel makePurchaseViewModel;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars =
            true // Light icons = false, Dark icons = true
        val intent = intent
        if (intent != null && intent.hasExtra(Constants.FROM_PRO)) {
            fromPro = intent.getBooleanExtra(Constants.FROM_PRO, false)
        }


        appBillingClient = AppBillingClient()
        loadSubscriptionDetails()
//        this.setWindowFlag()
        binding = PremiumActivtyNewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)

        //        setStatusBarColor(this, R.color.colorBlack);
        try {
            //binding.tvPrivacy.setOnClickListener(this);
            binding.llMonthly.setOnClickListener(this)
            binding.llWeekly.setOnClickListener(this)
            binding.ivClose.setOnClickListener(this)
            binding.tvUpgradeNow.setOnClickListener(this)

            // Set initial selection - weekly with trial is default
            setSelectedPlan(
                binding.llWeekly,
                isSelected = true
            )
            setSelectedPlan(
                binding.llMonthly,
                isSelected = false
            )

            // Initially hide trial text until subscription details are loaded
            binding.tvFreeTry.visibility = View.GONE
            // Default CTA for weekly until pricing arrives
            binding.tvUpgradeNow.text = getString(R.string.start_free_trial)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        lifecycleScope.launch {

            binding.ivClose.visibility = View.GONE
            val delay = RemoteConfigManager.getAdsConfig().premiumCloseBtnDelay.times(3000)
            Log.d(TAG, "delay: $delay")
            delay(delay.toLong())
            binding.ivClose.visibility = View.VISIBLE
        }
    }


    private fun setSelectedPlan(
        flCard: LinearLayout,
        isSelected: Boolean,
    ) {
        if (isSelected) {
            flCard.setBackgroundResource(R.drawable.bg_sku_selected)
        } else {
            flCard.setBackgroundResource(R.drawable.bg_sku_non_selected)
        }
    }


    override fun onClick(view: View) {
        val id = view.id
        when (id) {
//            R.id.card_yearly, R.id.tv_free_try -> {
//                analyticsManager.sendAnalytics("clicked", TAG + "free_try")
//                purchaseSubscription(Constants.SKU_SUBSCRIPTION_YEARLY)
//            }


            R.id.ll_weekly -> {
                isWeekly = true
                setSelectedPlan(
                    binding.llWeekly,
                    isSelected = true
                )
                setSelectedPlan(
                    binding.llMonthly,
                    isSelected = false
                )

                // Show trial text only if trial is available
                binding.tvFreeTry.visibility = if (hasTrial) View.VISIBLE else View.GONE
                // Update CTA text based on trial availability
                binding.tvUpgradeNow.text = if (hasTrial) {
                    getString(R.string.start_free_trial)
                } else {
                    getString(R.string.subscribe_weekly)
                }
            }

            R.id.ll_monthly -> {
                isWeekly = false
                setSelectedPlan(
                    binding.llWeekly,

                    isSelected = false
                )
                setSelectedPlan(
                    binding.llMonthly,
                    isSelected = true
                )

                // Hide trial text for monthly subscription
                binding.tvFreeTry.visibility = View.GONE
                // Update CTA text to Continue for monthly
                binding.tvUpgradeNow.text = getString(R.string.continuee)
            }

            R.id.tv_upgrade_now -> {
                if (!isWeekly) {

                    analyticsManager.sendAnalytics("clicked", TAG + "subscribe_monthly")
                    purchaseSubscription(Constants.SKU_SUBSCRIPTION_MONTHLY)
                } else {
                    analyticsManager.sendAnalytics("clicked", TAG + "subscribe_weekly")
                    purchaseSubscription(Constants.SKU_SUBSCRIPTION_WEEKLY)
                }
//                onBackPressed()
            }

            R.id.iv_close -> {
                analyticsManager.sendAnalytics("clicked", TAG + "btn_close")
                onBackPressed()
            }
        }
    }


    override fun onBackPressed() {
        if (fromPro) {
            super.onBackPressed()
        } else if (intent.getBooleanExtra(Constants.FROM_ONBOARDING, false)) {
            adMobManager.interstitialAdLoader.showAd(
                this,
                AdIds.getInterstitialAdID()
            ) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        } else if (intent.getBooleanExtra(Constants.IS_FROM_START_TO_PREMIUM, false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()

        } else {
            finish()
        }
    }

    private fun purchaseSubscription(sku: String) {
        if (skuDetail == null || skuDetail!!.isEmpty()) return
        for (item in skuDetail!!) {
            if (item.sku == sku) {
                appBillingClient!!.purchaseSkuItem(this, item)
            }
        }
    }

    private fun loadSubscriptionDetails() {
        if (appBillingClient != null) {
            appBillingClient!!.connect(
                this, object : ConnectResponse {
                    override fun disconnected() {
                    }

                    override fun billingUnavailable() {
                        Log.e("IAP", "billingUnavailable: ")
                    }

                    override fun developerError() {
                        Log.e("IAP", "billingUnavailable: ")
                    }

                    override fun error() {
                        Log.e("IAP", "error: ")
                    }

                    override fun featureNotSupported() {
                        Log.e("IAP", "featureNotSupported: ")
                    }

                    override fun itemUnavailable() {

                        Log.e("IAP", "itemUnavailable: ")
                    }

                    override fun ok(subscriptionItems: List<SubscriptionItem>) {
                        runOnUiThread {
                            skuDetail = subscriptionItems
                            for (item in subscriptionItems) {
                                Log.e("IAP", "ok: ${item.pricingPhase?.formattedPrice ?: ""}")

                                val price = item.pricingPhase?.formattedPrice ?: ""
                                when (item.sku) {
                                    Constants.SKU_SUBSCRIPTION_MONTHLY -> { // Monthly (assumed no trial)
                                        Log.e("IAP", "monthly")
                                        analyticsManager.sendAnalytics(
                                            "load",
                                            TAG + "monthly"
                                        )
                                        binding.tvMonthlyPrice.text = price
                                        binding.tvPrivacy.text =
                                            getString(R.string.cancel_anytime_at_least_24_hours_before_renewal_without_trial)

                                        // Hide trial text for monthly subscription
                                        if (!isWeekly) {
                                            binding.tvFreeTry.visibility = View.GONE
                                            // Ensure CTA shows Continue when monthly is currently selected
                                            binding.tvUpgradeNow.text =
                                                getString(R.string.continuee)
                                        }
                                    }

                                    Constants.SKU_SUBSCRIPTION_WEEKLY -> {
                                        if (item.isTrial == true) {
                                            Log.e("IAP", "Trail")
                                            hasTrial = true
                                            analyticsManager.sendAnalytics(
                                                "load",
                                                TAG + "3days_trial"
                                            )

                                            binding.tvPrivacy.text =
                                                getString(R.string.cancel_anytime_at_least_24_hours_before_renewal_trial)
                                            binding.tvFreeTry.text = getString(
                                                R.string.free_trial_disclaimer,
                                                price
                                            )
                                            binding.tvWeeklyPrice.text = price


                                            // Show trial text if weekly is selected
                                            if (isWeekly) {
                                                binding.tvFreeTry.visibility = View.VISIBLE
                                                binding.tvUpgradeNow.text =
                                                    getString(R.string.start_free_trial)
                                            }

                                        } else {
                                            Log.e("IAP", "weekly")
                                            hasTrial = false
                                            analyticsManager.sendAnalytics("load", TAG + "weekly")
                                            binding.tvWeeklyPrice.text = price
                                            binding.tvPrivacy.text =
                                                getString(R.string.cancel_anytime_at_least_24_hours_before_renewal_without_trial)
                                            binding.tvFreeTry.visibility = View.GONE
                                            if (isWeekly) {
                                                binding.tvUpgradeNow.text =
                                                    getString(R.string.subscribe_weekly)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


                    override fun serviceDisconnected() {
                    }

                    override fun serviceUnavailable() {
                    }
                },
                object : PurchaseResponse {
                    override fun isAlreadyOwned() {
                        appPreferences
                            .setBoolean(AppPreferences.IS_PREMIUM, true)
                    }

                    override fun userCancelled() {
                        appPreferences
                            .setBoolean(AppPreferences.IS_PREMIUM, false)
                    }

                    override fun ok(productItem: ProductItem) {
                        appPreferences.setBoolean(AppPreferences.IS_PREMIUM, true)

                        Toast.makeText(
                            this@PremiumActivity,
                            getString(R.string.purchase_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(this@PremiumActivity, StartActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }

                    override fun error(error: String) {
                        val dialogBuilder = AlertDialog.Builder(this@PremiumActivity)
                        dialogBuilder.setMessage(getString(R.string.premium_error_dialog_message))
                            .setCancelable(true)
                            .setPositiveButton("Continue") { dialog: DialogInterface?, id: Int -> }
                            .setNegativeButton("Cancel") { dialog: DialogInterface, id: Int -> dialog.cancel() }
                        val alert = dialogBuilder.create()
                        alert.show()
                    }
                })
        }
    }


    companion object {
        private const val TAG = "premium_activity_"
    }
}