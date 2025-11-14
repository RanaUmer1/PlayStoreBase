package com.professor.playstorebaseproject.app

/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com

 */


import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.professor.playstorebaseproject.iab.AppBillingClient
import com.professor.playstorebaseproject.iab.ProductItem
import com.professor.playstorebaseproject.iab.interfaces.ConnectResponse
import com.professor.playstorebaseproject.iab.interfaces.PurchaseResponse
import com.professor.playstorebaseproject.iab.subscription.SubscriptionItem
import com.professor.playstorebaseproject.remoteconfig.RemoteConfigManager
import com.professor.playstorebaseproject.utils.StatusBarUtils
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class Application : MultiDexApplication(), LifecycleObserver {


    @Inject
    lateinit var appPreferences: AppPreferences

    private lateinit var appBillingClient: AppBillingClient
    var skuDetail: SubscriptionItem? = null
    val TAG = "IAP Application Class"
    override fun onCreate() {
        super.onCreate()


        val appPreferences = AppPreferences(this)
        val savedLang = appPreferences.getString(AppPreferences.LANGUAGE_CODE)
        val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()

        if (savedLang.isNotEmpty() && savedLang != currentLang) {
            val localeList = LocaleListCompat.forLanguageTags(savedLang)
            AppCompatDelegate.setApplicationLocales(localeList)
        }

//        onetimeNotification()
//        scheduleRepeatingNotification()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appBillingClient = AppBillingClient()
        getSubscriptionDetails()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

                if (activity.javaClass.simpleName != "AdActivity")
                    StatusBarUtils.applyEdgeToEdge(activity)
            }

            override fun onActivityStarted(activity: Activity) {

            }

            override fun onActivityResumed(activity: Activity) {

            }

            override fun onActivityPaused(activity: Activity) {

            }

            override fun onActivityStopped(activity: Activity) {

            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

            }

            override fun onActivityDestroyed(activity: Activity) {

            }

        })
    }


    private fun onetimeNotification() {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
//            .setInitialDelay(
//                RemoteConfigManager.getNotificationTime(),
//                TimeUnit.HOURS
//            ) // optional first delay
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)

    }


    private fun scheduleRepeatingNotification() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            RemoteConfigManager.getNotificationTime(),
            TimeUnit.HOURS
        )
//            .setInitialDelay(
//                RemoteConfigManager.getNotificationTime(),
//                TimeUnit.HOURS
//            ) // optional first delay
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "repeating_notification",
            ExistingPeriodicWorkPolicy.KEEP, // don’t reschedule if already exists
            workRequest
        )
    }


    private fun getSubscriptionDetails() {
        appBillingClient.connect(this, object : ConnectResponse {
            override fun disconnected() {
                Log.d(
                    TAG,
                    "InappBilling connection disconnected."
                )
            }

            override fun billingUnavailable() {
                Log.d(
                    TAG,
                    "InappBilling billing unavailable."
                )
            }

            override fun developerError() {
                Log.d(
                    TAG,
                    "InappBilling developer error."
                )
            }

            override fun error() {
                Log.d(TAG, "InappBilling simple error.")
            }

            override fun featureNotSupported() {
                Log.d(
                    TAG,
                    "InappBilling feature not available."
                )
            }

            override fun itemUnavailable() {
                Log.d(
                    TAG,
                    "InappBilling item not available."
                )
            }


            override fun ok(subscriptionItems: List<SubscriptionItem>) {
                Log.d(
                    TAG,
                    "InappBilling connection ok do other ${subscriptionItems.size}."
                )
                for (it in subscriptionItems) {
                    if (it?.subscribedItem != null) {
                        skuDetail = it
                    }
                }

                if (skuDetail != null) {
                    if (skuDetail?.subscribedItem != null) {
                        appPreferences.setBoolean(AppPreferences.IS_PREMIUM, true)
                    } else {
                        appPreferences.setBoolean(AppPreferences.IS_PREMIUM, false)
                    }
                } else {
                    appPreferences.setBoolean(AppPreferences.IS_PREMIUM, false)
                }
            }

            override fun serviceDisconnected() {
                Log.d(
                    TAG,
                    "InappBilling service disconnected."
                )
            }

            override fun serviceUnavailable() {
                Log.d(
                    TAG,
                    "InappBilling service unavailable."
                )
            }
        }, object : PurchaseResponse {
            val isAlreadyOwned: Unit
                get() {
                    // Implement the logic for when the item is already owned.
                }

            override fun isAlreadyOwned() {

            }

            override fun userCancelled() {

                appPreferences.setBoolean(AppPreferences.IS_PREMIUM, false)
            }

            override fun ok(productItem: ProductItem) {

            }

            override fun error(error: String) {

            }


        })
    }
}