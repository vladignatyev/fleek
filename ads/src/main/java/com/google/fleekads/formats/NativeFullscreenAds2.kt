package com.google.fleekads.formats

import android.Manifest
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.fleekads.R
import com.google.fleekads.core.BaseAdLoader
import com.google.fleekads.core.NativeAdLoaderListener
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FullscreenAdsFragment2 : Fragment() {
    private var nativeAd: NativeAd? = null
    private var adsStateViewModel: FullscreenNativeAdStateViewModel? = null

    @LayoutRes
    private var layout: Int = R.layout.fullscreen_view
    private var delayBeforeSkipMs: Long = FullScreenAds2.DEFAULT_DELAY_BEFORE_SKIP

    private var timer: CountDownTimer? = null
    private var onFinished: (() -> Unit)? = null
    private var adToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        layout = args.getInt(ARG_LAYOUT_RES, R.layout.fullscreen_view)
        delayBeforeSkipMs = args.getLong(ARG_DELAY_MS, FullScreenAds2.DEFAULT_DELAY_BEFORE_SKIP)
        adToken = args.getString(ARG_AD_TOKEN)
        adsStateViewModel = ViewModelProvider(requireActivity())[FullscreenNativeAdStateViewModel::class.java]
        nativeAd = adToken?.let { adsStateViewModel?.peek(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ad = nativeAd ?: run {
            finalizeImpression()
            return View(requireContext())
        }

        val adView = inflater.inflate(layout, container, false)
        val skipButton = requireNotNull(adView.findViewById<Button>(R.id.skip)) {
            "Fullscreen Ads Layout should contain skip button."
        }
        val countdownView = adView.findViewById<TextView?>(R.id.countdown_timer)
        val usesDedicatedCountdown = countdownView != null
        val countdownTarget = countdownView ?: skipButton

        configureSkipControls(
            root = adView,
            skipButton = skipButton,
            countdownView = countdownTarget,
            usesDedicatedCountdown = usesDedicatedCountdown
        )

        com.google.fleekads.core.NativeAdsBinding(adView).populate(ad)
        adView.setBackgroundColor(
            ContextCompat.getColor(requireContext(), com.google.fleekads.R.color.fullscreen_ads_background_color)
        )

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (skipButton.isEnabled) {
                finalizeImpression()
            }
        }
        return adView
    }

    private fun configureSkipControls(
        root: View,
        skipButton: Button,
        countdownView: TextView,
        usesDedicatedCountdown: Boolean
    ) {
        val skipLabel = skipButton.text
        if (usesDedicatedCountdown) {
            skipButton.visibility = View.GONE
        } else {
            skipButton.isEnabled = false
        }

        skipButton.text = getString(R.string.skip_ads_btn_label)
        skipButton.setOnClickListener {
            if (usesDedicatedCountdown) {
                root.visibility = View.GONE
            }
            finalizeImpression()
        }

        var secondsRemaining = delayBeforeSkipMs / 1000
        countdownView.text = getString(R.string.skip_ads_text, secondsRemaining)

        timer?.cancel()
        timer = object : CountDownTimer(delayBeforeSkipMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isAdded) return
                secondsRemaining = millisUntilFinished / 1000 + 1
                countdownView.text = getString(R.string.skip_ads_text, secondsRemaining)
            }

            override fun onFinish() {
                if (!isAdded) return
                if (usesDedicatedCountdown) {
                    countdownView.visibility = View.GONE
                    skipButton.visibility = View.VISIBLE
                } else {
                    skipButton.isEnabled = true
                    skipButton.text = skipLabel
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        timer?.cancel()
        timer = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        if (activity?.isChangingConfigurations != true) {
            releaseAd(destroy = true)
        }
        super.onDestroy()
    }

    fun setOnFinishedListener(listener: () -> Unit) {
        onFinished = listener
    }

    fun finalizeImpression() {
        if (!isAdded || parentFragmentManager.isStateSaved) return

        releaseAd(destroy = true)
        parentFragmentManager.popBackStack(
            FullScreenAds2.FullscreenAdPresenter2.BACKSTACK_FRAGMENT_NAME,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        onFinished?.invoke()
    }

    private fun releaseAd(destroy: Boolean) {
        val token = adToken ?: return
        val releasedAd = adsStateViewModel?.remove(token)
        adToken = null
        nativeAd = null
        if (destroy) {
            releasedAd?.destroy()
        }
    }

    companion object {
        private const val ARG_AD_TOKEN = "arg_ad_token"
        private const val ARG_LAYOUT_RES = "arg_layout_res"
        private const val ARG_DELAY_MS = "arg_delay_ms"

        fun newInstance(
            adToken: String,
            @LayoutRes layoutRes: Int,
            delayBeforeSkipMs: Long
        ): FullscreenAdsFragment2 = FullscreenAdsFragment2().apply {
            arguments = Bundle().apply {
                putString(ARG_AD_TOKEN, adToken)
                putInt(ARG_LAYOUT_RES, layoutRes)
                putLong(ARG_DELAY_MS, delayBeforeSkipMs)
            }
        }
    }
}

internal class FullscreenNativeAdStateViewModel : ViewModel() {
    private val ads = LinkedHashMap<String, NativeAd>()

    fun put(token: String, nativeAd: NativeAd) {
        ads[token] = nativeAd
    }

    fun peek(token: String): NativeAd? = ads[token]

    fun remove(token: String): NativeAd? = ads.remove(token)

    override fun onCleared() {
        ads.values.forEach { it.destroy() }
        ads.clear()
        super.onCleared()
    }
}

class FullScreenAds2(
    private val placement: BaseAdLoader<NativeAd>,
    private val delayBeforeSkipMs: Long = DEFAULT_DELAY_BEFORE_SKIP,
    @LayoutRes val layout: Int = R.layout.fullscreen_view,
    private val timeoutForAdCall: Duration = DEFAULT_TIMEOUT_FOR_AD_CALL
) {
    companion object {
        const val DEFAULT_DELAY_BEFORE_SKIP = 3000L
        val DEFAULT_TIMEOUT_FOR_AD_CALL = 6.seconds
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    suspend fun preload(
        fragmentActivity: FragmentActivity,
        listener: NativeAdLoaderListener
    ): FullscreenAdPresenter2? {
        val presentationAwareListener = object : NativeAdLoaderListener(listener.adLoader) {
            override fun onLoading() = listener.onLoading()
            override fun onAdLoaded(ad: NativeAd) = listener.onAdLoaded(ad)
            override fun onAdClicked() = listener.onAdClicked()
            override fun onAdFailedToLoad(error: LoadAdError) = listener.onAdFailedToLoad(error)
        }

        val preloaded = placement.awaitLoadedWithTimeout(
            activity = fragmentActivity,
            listener = presentationAwareListener,
            timeout = timeoutForAdCall
        ) ?: return null

        return FullscreenAdPresenter2(
            nativeAd = preloaded,
            fragmentActivity = fragmentActivity,
            layoutRes = layout,
            delayBeforeSkip = delayBeforeSkipMs
        )
    }

    class FullscreenAdPresenter2(
        private val nativeAd: NativeAd,
        private val fragmentActivity: FragmentActivity,
        @LayoutRes private val layoutRes: Int,
        private val delayBeforeSkip: Long
    ) {
        companion object {
            const val BACKSTACK_FRAGMENT_NAME = "FullscreenAds2"
            private const val FRAGMENT_TAG = "NativeFullscreenAds2"
        }

        private var wasShown = false

        fun show(@IdRes fragmentContainer: Int, listener: () -> Unit) {
            if (wasShown) return
            val fm = fragmentActivity.supportFragmentManager
            if (fragmentActivity.isFinishing || fragmentActivity.isDestroyed || fm.isDestroyed) return

            val adStateViewModel = ViewModelProvider(fragmentActivity)[FullscreenNativeAdStateViewModel::class.java]
            val adToken = "fullscreen_native_" + System.identityHashCode(nativeAd) + "_" + System.nanoTime()
            adStateViewModel.put(adToken, nativeAd)

            fun showAction() {
                if (wasShown || fm.isStateSaved || fm.findFragmentByTag(FRAGMENT_TAG) != null) return

                val managedFragment = FullscreenAdsFragment2.newInstance(
                    adToken = adToken,
                    layoutRes = layoutRes,
                    delayBeforeSkipMs = delayBeforeSkip
                ).apply {
                    setOnFinishedListener(listener)
                }

                fm.beginTransaction()
                    .add(fragmentContainer, managedFragment, FRAGMENT_TAG)
                    .addToBackStack(BACKSTACK_FRAGMENT_NAME)
                    .commit()
                wasShown = true
            }

            if (!fm.isStateSaved && fragmentActivity.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                showAction()
            } else {
                fragmentActivity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onResume(owner: LifecycleOwner) {
                        owner.lifecycle.removeObserver(this)
                        if (!fm.isStateSaved) {
                            showAction()
                        }
                    }

                    override fun onDestroy(owner: LifecycleOwner) {
                        owner.lifecycle.removeObserver(this)
                        if (!wasShown) {
                            adStateViewModel.remove(adToken)?.destroy()
                        }
                    }
                })
            }
        }
    }
}
