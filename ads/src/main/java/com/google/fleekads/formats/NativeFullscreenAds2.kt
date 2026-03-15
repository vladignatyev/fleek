package com.google.fleekads.formats

import android.Manifest
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.fleekads.R
import com.google.fleekads.core.BaseAdLoader
import com.google.fleekads.core.NativeAdLoaderListener
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FullscreenAdsFragment2 : Fragment() {
    lateinit var nativeAd: NativeAd

    @LayoutRes
    var layout: Int = R.layout.fullscreen_view
    var delayBeforeSkipMs: Long = FullScreenAds2.DEFAULT_DELAY_BEFORE_SKIP

    private var timer: CountDownTimer? = null
    private var onFinished: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        com.google.fleekads.core.NativeAdsBinding(adView).populate(nativeAd)
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
        if (this::nativeAd.isInitialized) {
            nativeAd.destroy()
        }
        super.onDestroy()
    }

    fun setOnFinishedListener(listener: () -> Unit) {
        onFinished = listener
    }

    fun finalizeImpression() {
        if (!isAdded || parentFragmentManager.isStateSaved) return

        parentFragmentManager.popBackStack(
            FullScreenAds2.FullscreenAdPresenter2.BACKSTACK_FRAGMENT_NAME,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        onFinished?.invoke()
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

            fun showAction() {
                if (wasShown || fm.isStateSaved || fm.findFragmentByTag(FRAGMENT_TAG) != null) return

                val managedFragment = FullscreenAdsFragment2().apply {
                    nativeAd = this@FullscreenAdPresenter2.nativeAd
                    layout = layoutRes
                    delayBeforeSkipMs = delayBeforeSkip
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
                })
            }
        }
    }
}
