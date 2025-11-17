package com.professor.playstorebaseproject.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
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
import com.professor.playstorebaseproject.iab.ConnectResponse
import com.professor.playstorebaseproject.iab.PurchaseResponse
import com.professor.playstorebaseproject.iab.SubscriptionItem
import com.professor.playstorebaseproject.remoteconfig.RemoteConfigManager
import com.professor.playstorebaseproject.ui.viewmodel.PremiumViewModel

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

    private lateinit var binding: PremiumActivtyNewBinding
    private val viewModel: PremiumViewModel by viewModels()

    private lateinit var billingClient: AppBillingClient
    private var availableSubscriptions: List<SubscriptionItem> = emptyList()

    private var fromPro: Boolean = false
    private var isWeekly: Boolean = true
    private var hasTrial: Boolean = false

    private val TAG = "PremiumActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup window insets
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true

        binding = PremiumActivtyNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeActivity()
        setupClickListeners()
        initializeBilling()
        setupObservers()

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, TAG)
    }

    private fun initializeActivity() {
        fromPro = intent.getBooleanExtra(Constants.FROM_PRO, false)

        // Get billing client instance
        billingClient = AppBillingClient.getInstance()

        // Set initial UI state
        setInitialUIState()

        // Setup close button delay
        setupCloseButtonDelay()
    }

    private fun setupClickListeners() {
        binding.llMonthly.setOnClickListener(this)
        binding.llWeekly.setOnClickListener(this)
        binding.ivClose.setOnClickListener(this)
        binding.tvUpgradeNow.setOnClickListener(this)
    }

    private fun setInitialUIState() {
        // Set initial selection - weekly with trial is default
        setSelectedPlan(binding.llWeekly, isSelected = true)
        setSelectedPlan(binding.llMonthly, isSelected = false)

        // Initially hide trial text until subscription details are loaded
        binding.tvFreeTry.visibility = View.GONE
        // Default CTA for weekly until pricing arrives
        binding.tvUpgradeNow.text = getString(R.string.start_free_trial)
        binding.tvUpgradeNow.isEnabled = false // Disable until subscriptions are loaded
    }

    private fun setupCloseButtonDelay() {
        lifecycleScope.launch {
            binding.ivClose.visibility = View.GONE
            val delay = RemoteConfigManager.getAdsConfig().premiumCloseBtnDelay.times(3000)
            Log.d(TAG, "Close button delay: $delay ms")
            delay(delay.toLong())
            binding.ivClose.visibility = View.VISIBLE
        }
    }

    private fun initializeBilling() {
        billingClient.initialize(this, object : ConnectResponse {
            override fun onConnected(subscriptionItems: List<SubscriptionItem>) {
                runOnUiThread {
                    availableSubscriptions = subscriptionItems
                    updateUIWithSubscriptions(subscriptionItems)
                    binding.tvUpgradeNow.isEnabled = true
                    Log.d(TAG, "Billing connected, ${subscriptionItems.size} subscriptions loaded")
                }
            }

            override fun onDisconnected() {
                runOnUiThread {
                    showError("Billing service disconnected. Please try again.")
                    binding.tvUpgradeNow.isEnabled = false
                }
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                runOnUiThread {
                    showError("Billing error: $errorMessage")
                    binding.tvUpgradeNow.isEnabled = false
                    Log.e(TAG, "Billing error: $errorCode - $errorMessage")
                }
            }
        })
    }

    private fun setupObservers() {
        // You can add ViewModel observers here if needed
    }

    private fun updateUIWithSubscriptions(subscriptions: List<SubscriptionItem>) {
        var weeklySubscription: SubscriptionItem? = null
        var monthlySubscription: SubscriptionItem? = null

        // Find weekly and monthly subscriptions
        subscriptions.forEach { subscription ->
            when (subscription.sku) {
                Constants.SKU_SUBSCRIPTION_WEEKLY -> weeklySubscription = subscription
                Constants.SKU_SUBSCRIPTION_MONTHLY -> monthlySubscription = subscription
            }
        }

        // Update weekly subscription UI
        weeklySubscription?.let { weekly ->
            val price = weekly.formattedPrice ?: ""
            binding.tvWeeklyPrice.text = price

            if (weekly.hasFreeTrial) {
                hasTrial = true
                analyticsManager.sendAnalytics("load", "${TAG}3days_trial")
                binding.tvFreeTry.text = getString(R.string.free_trial_disclaimer, price)
                binding.tvPrivacy.text = getString(R.string.cancel_anytime_at_least_24_hours_before_renewal_trial)

                if (isWeekly) {
                    binding.tvFreeTry.visibility = View.VISIBLE
                    binding.tvUpgradeNow.text = getString(R.string.start_free_trial)
                }
            } else {
                hasTrial = false
                analyticsManager.sendAnalytics("load", "${TAG}weekly")
                binding.tvFreeTry.visibility = View.GONE
                if (isWeekly) {
                    binding.tvUpgradeNow.text = getString(R.string.subscribe_weekly)
                }
                binding.tvPrivacy.text = getString(R.string.cancel_anytime_at_least_24_hours_before_renewal_without_trial)
            }
        }

        // Update monthly subscription UI
        monthlySubscription?.let { monthly ->
            val price = monthly.formattedPrice ?: ""
            binding.tvMonthlyPrice.text = price
            analyticsManager.sendAnalytics("load", "${TAG}monthly")

            if (!isWeekly) {
                binding.tvFreeTry.visibility = View.GONE
                binding.tvUpgradeNow.text = getString(R.string.continuee)
            }
        }

        Log.d(TAG, "UI updated with subscription details")
    }

    private fun setSelectedPlan(planLayout: LinearLayout, isSelected: Boolean) {
        if (isSelected) {
            planLayout.setBackgroundResource(R.drawable.bg_sku_selected)
        } else {
            planLayout.setBackgroundResource(R.drawable.bg_sku_non_selected)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.ll_weekly -> {
                handleWeeklySelection()
            }

            R.id.ll_monthly -> {
                handleMonthlySelection()
            }

            R.id.tv_upgrade_now -> {
                handleUpgradeNow()
            }

            R.id.iv_close -> {
                analyticsManager.sendAnalytics("clicked", "${TAG}btn_close")
                onBackPressed()
            }
        }
    }

    private fun handleWeeklySelection() {
        isWeekly = true
        setSelectedPlan(binding.llWeekly, isSelected = true)
        setSelectedPlan(binding.llMonthly, isSelected = false)

        // Show trial text only if trial is available
        binding.tvFreeTry.visibility = if (hasTrial) View.VISIBLE else View.GONE

        // Update CTA text based on trial availability
        binding.tvUpgradeNow.text = if (hasTrial) {
            getString(R.string.start_free_trial)
        } else {
            getString(R.string.subscribe_weekly)
        }

        analyticsManager.sendAnalytics("clicked", "${TAG}select_weekly")
    }

    private fun handleMonthlySelection() {
        isWeekly = false
        setSelectedPlan(binding.llWeekly, isSelected = false)
        setSelectedPlan(binding.llMonthly, isSelected = true)

        // Hide trial text for monthly subscription
        binding.tvFreeTry.visibility = View.GONE

        // Update CTA text to Continue for monthly
        binding.tvUpgradeNow.text = getString(R.string.continuee)

        analyticsManager.sendAnalytics("clicked", "${TAG}select_monthly")
    }

    private fun handleUpgradeNow() {
        val sku = if (isWeekly) {
            analyticsManager.sendAnalytics("clicked", "${TAG}subscribe_weekly")
            Constants.SKU_SUBSCRIPTION_WEEKLY
        } else {
            analyticsManager.sendAnalytics("clicked", "${TAG}subscribe_monthly")
            Constants.SKU_SUBSCRIPTION_MONTHLY
        }

        purchaseSubscription(sku)
    }

    private fun purchaseSubscription(sku: String) {
        val subscription = availableSubscriptions.find { it.sku == sku }
        if (subscription == null) {
            showError("Subscription not available. Please try again.")
            return
        }

        // Get the appropriate offer token (base offer for now)
        val offerToken = subscription.baseOfferToken
        if (offerToken.isNullOrEmpty()) {
            showError("Unable to process subscription. Please try again.")
            return
        }

        billingClient.purchaseSubscription(
            this,
            subscription,
            offerToken,
            object : PurchaseResponse {
                override fun onPurchaseSuccess(productId: String) {
                    runOnUiThread {
                        handlePurchaseSuccess(productId)
                    }
                }

                override fun onPurchasePending() {
                    runOnUiThread {
                        showMessage("Purchase pending...")
                    }
                }

                override fun onPurchaseCancelled() {
                    runOnUiThread {
                        showMessage("Purchase cancelled")
                        analyticsManager.sendAnalytics("purchase_cancelled", "${TAG}$sku")
                    }
                }

                override fun onPurchaseAlreadyOwned() {
                    runOnUiThread {
                        handlePurchaseSuccess(sku)
                        showMessage("You already own this subscription")
                    }
                }

                override fun onPurchaseError(errorCode: Int, errorMessage: String) {
                    runOnUiThread {
                        showError("Purchase failed: $errorMessage")
                        analyticsManager.sendAnalytics("purchase_error", "${TAG}${errorCode}_$sku")
                        Log.e(TAG, "Purchase error $errorCode: $errorMessage")
                    }
                }
            }
        )
    }

    private fun handlePurchaseSuccess(productId: String) {
        // Update premium status
        appPreferences.setBoolean(AppPreferences.IS_PREMIUM, true)
        AdMobManager.isPremium = true

        // Show success message
        Toast.makeText(
            this,
            getString(R.string.purchase_successfully),
            Toast.LENGTH_SHORT
        ).show()

        analyticsManager.sendAnalytics("purchase_success", "${TAG}$productId")

        // Navigate to appropriate screen
        navigateAfterPurchase()
    }

    private fun navigateAfterPurchase() {
        val intent = when {
            fromPro -> {
                // If coming from pro features, go back
                null
            }
            intent.getBooleanExtra(Constants.FROM_ONBOARDING, false) -> {
                // If coming from onboarding, go to main
                Intent(this, MainActivity::class.java)
            }
            intent.getBooleanExtra(Constants.IS_FROM_START_TO_PREMIUM, false) -> {
                // If coming from start, go to main
                Intent(this, MainActivity::class.java)
            }
            else -> {
                // Default navigation
                Intent(this, StartActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }

        intent?.let {
            startActivity(it)
        }
        finish()
    }

    override fun onBackPressed() {
        when {
            fromPro -> {
                super.onBackPressed()
            }
            intent.getBooleanExtra(Constants.FROM_ONBOARDING, false) -> {
                showInterstitialAndNavigate()
            }
            intent.getBooleanExtra(Constants.IS_FROM_START_TO_PREMIUM, false) -> {
                navigateToMain()
            }
            else -> {
                finish()
            }
        }
    }

    private fun showInterstitialAndNavigate() {
        adMobManager.interstitialAdLoader.showAd(
            this,
            AdIds.getInterstitialAdID()
        ) {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e(TAG, message)
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.d(TAG, message)
    }

    override fun onDestroy() {
        super.onDestroy()
        billingClient.disconnect()
        Log.d(TAG, "PremiumActivity destroyed")
    }
}