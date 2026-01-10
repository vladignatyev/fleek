package com.github.vladignatyev.fleek.ads_example

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.vladignatyev.fleek.ads_example.databinding.MainActivityBinding
import com.google.fleekads.core.AdListenerWithRevenue
import com.google.fleekads.core.ChainNativeAdLoader
import com.google.fleekads.core.NativeAdChain
import com.google.fleekads.formats.AppOpenAdCallback
import com.google.fleekads.formats.AppOpenPlacement
import com.google.fleekads.formats.BasicNativePlacement
import com.google.fleekads.formats.FullScreenAds
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_LEFT
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


class MainActivity : AppCompatActivity(R.layout.main_activity) {
    private lateinit var binding: MainActivityBinding

    // App open ad placement.
    lateinit var appOpenAdsPlacement: AppOpenPlacement

    // Daisy-chained native ad placement to use in Fullscreen Native Ad
    private var daisyChainedFullscreenNativePlacement: BasicNativePlacement = BasicNativePlacement(
        ChainNativeAdLoader(
            individualCallTimeout = 5000.milliseconds,
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


                    MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf("3BB3D2B06C43243B0384C4DDD40987F4")).build()
            )
        MobileAds.initialize(this@MainActivity) { initializationStatus ->
//            for (entry in initializationStatus.adapterStatusMap.entries) {
//                val adapterClass = entry.key
//                val status: AdapterStatus = entry.value!!
//                Log.d(
//                    "Ads",
//                    String.format(
//                        "Adapter name: %s, Description: %s, Latency: %d",
//                        adapterClass, status.description, status.latency
//                    )
//                )
//            }
            initAppOpen()

            showContent()

        }

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

            if (presenter == null) {
                Toast.makeText(this@MainActivity, "No fullscreen ads loaded.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
