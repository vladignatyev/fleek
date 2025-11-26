package com.github.vladignatyev.fleek.ads_example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.vladignatyev.fleek.ads_example.databinding.MainActivityBinding
import com.google.ads.InterstitialPlacement
import com.google.ads.attribution.DummyAttribution
import com.google.ads.attribution.TenjinAttributionWithAdRevenue
import com.google.ads.core.AdListenerWithRevenue
import com.google.ads.core.BasicNativeAdLoader
import com.google.ads.core.ChainNativeAdLoader
import com.google.ads.core.NativeAdChain
import com.google.ads.formats.AppOpenAdCallback
import com.google.ads.formats.AppOpenPlacement
import com.google.ads.formats.BasicNativePlacement
import com.google.ads.formats.FullScreenAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_LEFT
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


class MainActivity : AppCompatActivity(R.layout.main_activity) {
    private lateinit var binding: MainActivityBinding

    // App open ad placement.
    lateinit var appOpenAdsPlacement: AppOpenPlacement

    // Daisy-chained native ad placement to use in Fullscreen Native Ad
    private var daisyChainedFullscreenNativePlacement: BasicNativePlacement = BasicNativePlacement(
        ChainNativeAdLoader(
            individualCallTimeout = 10.milliseconds,
            chain = NativeAdChain.fromList(
                nativeChainAIDs,
                nativeAdOptions = NativeAdOptions.Builder()
                    .setAdChoicesPlacement(ADCHOICES_TOP_LEFT)
                    .setVideoOptions(VideoOptions.Builder().setStartMuted(false).build())
                    .build()
            )
        )
    )

    // Fullscreen Native Ad definition.
    private var fullscreenNativeAds: FullScreenAds = FullScreenAds(
        placement = daisyChainedFullscreenNativePlacement,
        layout = com.google.ads.R.layout.fullscreen_view
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Uncomment this if you plan to use Tenjin as an attribution provider
        // attribution = TenjinAttributionWithAdRevenue.create(this, TENJIN_SDK_KEY)

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initAppOpen()

        showContent()
    }

    private fun showContent() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, DemoFragment())
            .commit()
    }

    private fun initAppOpen() {
        // Initialize placement...
        appOpenAdsPlacement = AppOpenPlacement(
            activityClass = MainActivity::class,
            application = this.application,
            adUnitId = appOpenAID,
            ilrdFactory = attribution,
            adCallback = AppOpenAdCallback()
        )

        // ...and enable tracking of the lifecycle events.
        appOpenAdsPlacement.startTrackResume()
    }

    override fun onResume() {
        super.onResume()
        // Uncomment this if you plan to use Tenjin as an attribution provider:
        // attribution.connectOnResume()
    }

    fun loadAndShowFullscreenNativeAds() {
        appOpenAdsPlacement.stopTrackResume()
        lifecycleScope.launch {
            // Preload.
            val presenter = fullscreenNativeAds.preload(
                fragmentActivity = this@MainActivity,
                listener = AdListenerWithRevenue(
                    adLoader = daisyChainedFullscreenNativePlacement,
                    ilrdFactory = attribution
                )
            )
            // Then show.
            presenter?.show(R.id.fragmentContainer) {
                // This code will run after ad impression handler triggered.
                appOpenAdsPlacement.startTrackResume()
            }
        }
    }
}
