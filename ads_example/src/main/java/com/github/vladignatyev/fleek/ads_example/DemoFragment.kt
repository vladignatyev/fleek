package com.github.vladignatyev.fleek.ads_example

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.github.vladignatyev.fleek.ads_example.databinding.FragmentDemoBinding
import com.github.vladignatyev.fleek.ads_example.databinding.MainActivityBinding
import com.google.ads.InterstitialPlacement
import com.google.ads.OnAdDismissed
import com.google.ads.core.AdListenerWithRevenue
import com.google.ads.core.BasicNativeAdLoader
import com.google.ads.core.ChainNativeAdLoader
import com.google.ads.core.NativeAdChain
import com.google.ads.core.NativeAdsBinding
import com.google.ads.formats.AppOpenAdCallback
import com.google.ads.formats.AppOpenPlacement
import com.google.ads.formats.BasicNativePlacement
import com.google.ads.formats.FullScreenAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_LEFT
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class DemoFragment : Fragment() {
    private lateinit var binding: FragmentDemoBinding

    // Interstitial Ad
    private var interstitialThatLoadsOnClick: InterstitialPlacement =
        InterstitialPlacement(interAID, ilrdFactory = attribution)

    // Simple native ad placement.
    private var simpleNativeInContent: BasicNativePlacement = BasicNativePlacement(
        BasicNativeAdLoader(
            nativeAID, nativeAdOptions = NativeAdOptions.Builder()
                .setVideoOptions(VideoOptions.Builder().setStartMuted(false).build())
                .build()
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDemoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity != null && (activity?.isDestroyed == true || activity?.isFinishing == true)) {
            return
        }
        interstitialThatLoadsOnClick.load(requireActivity())
    }

    private fun setupListeners() {
        binding.loadAndShowNativeAdButton.setOnClickListener {
            lifecycleScope.launch {
                val activity = requireActivity()
                // Load.
                val ads = simpleNativeInContent.awaitLoadedWithTimeout(
                    activity = activity, listener = AdListenerWithRevenue(
                        simpleNativeInContent, attribution
                    ), timeout = 10.seconds
                )

                // If ads loaded...
                ads?.let {
                    // Create binding by providing a root view of ads layout.
                    val adBinding = NativeAdsBinding(binding.nativeAd)

                    // Show ads.
                    adBinding.populate(it)
                }
            }
        }

        binding.loadAndShowFullscreenNativeAdButton.setOnClickListener {
            lifecycleScope.launch {
                (activity as MainActivity).loadAndShowFullscreenNativeAds()
            }
        }

        binding.loadAndShowInterstitial.setOnClickListener {
            lifecycleScope.launch {
                if (activity != null && (activity?.isDestroyed == true || activity?.isFinishing == true)) {
                    return@launch
                }

                (activity as MainActivity).appOpenAdsPlacement.stopTrackResume()

                interstitialThatLoadsOnClick.present(requireActivity(), object: OnAdDismissed{
                    override fun onAdClosed() {
                        (activity as MainActivity).appOpenAdsPlacement.startTrackResume()
                    }
                })
            }
        }
    }
}