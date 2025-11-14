package com.professor.playstorebaseproject.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mzalogics.ads.domain.ads.native_ad.NativeAdBuilder
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.playstorebaseproject.R
import com.professor.playstorebaseproject.databinding.FullNativeAdItemViewPagerBinding
import com.professor.playstorebaseproject.databinding.ItemOnboardingBinding
import com.professor.playstorebaseproject.app.AdIds
import com.professor.playstorebaseproject.model.OnboardingItem
import com.professor.playstorebaseproject.remoteconfig.RemoteConfigManager


class OnboardingAdapter(
    private val items: List<OnboardingItem>,
    private var adMobManager: AdMobManager
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ONBOARDING = 0
        private const val VIEW_TYPE_AD = 1
    }

    inner class OnboardingViewHolder(val binding: ItemOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OnboardingItem) {
            binding.tvTitle.text = item.title
            binding.tvSubtitle.text = item.description
            Glide.with(binding.ivMainImage)
                .load(item.imageRes)
                .into(binding.ivMainImage)
        }
    }

    inner class AdViewHolder(val binding: FullNativeAdItemViewPagerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            // Here you will load & populate your NativeAd
            // Example (pseudo):
            // val adLoader = AdLoader.Builder(binding.root.context, "YOUR_AD_UNIT_ID")
            //    .forNativeAd { nativeAd -> populateNativeAdView(nativeAd, binding.nativeAdView) }
            //    .build()
            // adLoader.loadAd(AdRequest.Builder().build())

            Log.e("TAG", "bind: full native", )
            loadFullNativeAd(adMobManager, binding)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 1) VIEW_TYPE_AD else VIEW_TYPE_ONBOARDING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ONBOARDING) {
            val binding = ItemOnboardingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            OnboardingViewHolder(binding)
        } else {
            val binding = FullNativeAdItemViewPagerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            AdViewHolder(binding)
        }
    }

    override fun getItemCount() = items.size + 1 // +1 for ad

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_ONBOARDING) {
            val actualPos = if (position > 1) position - 1 else position
            (holder as OnboardingViewHolder).bind(items[actualPos])
        } else {
            (holder as AdViewHolder).bind()
        }
    }


    private fun loadFullNativeAd(
        adMobManager: AdMobManager,
        binding: FullNativeAdItemViewPagerBinding
    ) {
        if (adMobManager.nativeAdLoader.isAdLoaded()) {
            Log.e("TAG", "load full native ")
            adMobManager.nativeAdLoader.showLoadedAd(
                NativeAdBuilder.Builder(
                    R.layout.full_native_ad_design,
                    binding.includeAd.adFrame,
                    binding.includeAd.shimmerFbAd
                ).setShowBody(true)
                    .setShowMedia(true)
                    .setAdTitleColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].heading)
                    .setAdBodyColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].description)
                    .setCtaTextColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].ctaText)
                    .setCtaBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].callActionButtonColor)
                   // .setAdBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].backgroundColor)
                    .build(), AdIds.getFullNativeOnboardingAdId()
            )
        } else {
            Log.e("TAG", "load and show: full native  ")
            adMobManager.nativeAdLoader.loadAndShow(
                AdIds.getFullNativeOnboardingAdId(),
                NativeAdBuilder.Builder(
                    R.layout.full_native_ad_design,
                    binding.includeAd.adFrame,
                    binding.includeAd.shimmerFbAd
                ).setShowBody(true)
                    .setShowMedia(true)
                    .setAdTitleColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].heading)
                    .setAdBodyColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].description)
                    .setCtaTextColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].ctaText)
                    .setCtaBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].callActionButtonColor)
                 //   .setAdBgColor(RemoteConfigManager.getAdsConfig().nativeConfig[0].backgroundColor)
                    .build()
            ) {}

        }
    }
}
