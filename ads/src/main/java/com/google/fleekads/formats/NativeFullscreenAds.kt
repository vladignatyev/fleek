package com.google.fleekads.formats

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
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
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.nativead.NativeAd
import com.google.fleekads.R
import com.google.fleekads.core.NativeAdsBinding
import com.google.fleekads.core.BaseAdLoader
import com.google.fleekads.core.NativeAdLoaderListener
import com.google.fleekads.formats.FullScreenAds.Companion.DEFAULT_DELAY_BEFORE_SKIP
import com.google.fleekads.formats.FullScreenAds.FullscreenAdPresenter.Companion.BACKSTACK_FRAGMENT_NAME
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


class FullscreenAdsFragment : Fragment() {
    lateinit var nativeAd: NativeAd

    @LayoutRes
    var layout: Int = R.layout.fullscreen_view

    var delayBeforeSkip: Long = DEFAULT_DELAY_BEFORE_SKIP

    private lateinit var adsBinding: NativeAdsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        try {
            val adView = inflater.inflate(layout, container, false)

            adView.let { parent ->
                val delayToShowNextButton = delayBeforeSkip

                var countDownTimer = parent.findViewById<TextView>(R.id.countdown_timer)
                val skip = parent.findViewById<Button>(R.id.skip)
                val skipText = skip.text
                val layoutWithSeparateCountdown = countDownTimer != null

                if (skip == null) {
                    throw IllegalArgumentException("Fullscreen Ads Layout should contain skip button.")
                }
                if (countDownTimer == null) {
                    countDownTimer = skip
                }


                skip.apply {
                    if (layoutWithSeparateCountdown) visibility = View.GONE
                    text = context.getString(R.string.skip_ads_btn_label)
                    setOnClickListener {
                        if (layoutWithSeparateCountdown) parent.visibility = View.GONE
                        finalizeImpression()
                    }
                    if (!layoutWithSeparateCountdown) {
                        isEnabled = false
                    }
                }

                var secondsRemaining = delayToShowNextButton / 1000

                countDownTimer.apply {
                    text = context.getString(R.string.skip_ads_text, secondsRemaining)
                }

                val countTimer = object : CountDownTimer(delayToShowNextButton, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        if (!isAdded)
                            return
                        secondsRemaining = millisUntilFinished / 1000 + 1
                        countDownTimer.text = this@FullscreenAdsFragment.getString(
                            R.string.skip_ads_text, secondsRemaining
                        )
                    }

                    override fun onFinish() {
                        if (!isAdded)
                            return

                        if (layoutWithSeparateCountdown) {
                            countDownTimer.visibility = View.GONE
                            skip.visibility = View.VISIBLE
                        } else {
                            skip.isEnabled = true
                            skip.text = skipText
                        }
                    }
                }
                countTimer.start()

                adsBinding = NativeAdsBinding(adView)
                adsBinding.populate(nativeAd)
            }

//            val id = com.google.fleekads.R.color.fullscreen_ads_background_color
//
//            val idApp = R.color.fullscreen_ads_background_color
//            val idLib = com.google.fleekads.R.color.fullscreen_ads_background_color
//
//            val ctx = requireContext()
//            Log.e("CTX", "pkg=${ctx.packageName} resPkg=${ctx.resources.getResourcePackageName(R.color.fullscreen_ads_background_color)}")
//            Log.e("RES", "id=$id name=${resources.getResourceName(id)} type=${resources.getResourceTypeName(id)}")
//
////            val csl = ContextCompat.getColor(this.requireActivity(), com.google.fleekads.R.color.fullscreen_ads_background_color)
//
            adView.setBackgroundColor(
                ContextCompat.getColor(requireContext(), com.google.fleekads.R.color.fullscreen_ads_background_color)
            )

            return adView
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            finalizeImpression()
            return null
        }
    }


    private fun finalizeImpression() {
        parentFragmentManager.popBackStack(
            BACKSTACK_FRAGMENT_NAME, FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        listener?.onFinishedListener()
    }

    open class OnFinishedListener {
        open fun onFinishedListener() {}
    }

    var listener: OnFinishedListener? = null

    fun setOnFinishedListener(listener: () -> Unit) {
        this@FullscreenAdsFragment.listener = object : OnFinishedListener() {
            override fun onFinishedListener() {
                listener()
            }
        }
    }

    companion object {
        const val TAG = "FullscreenAdsFragment"
    }
}

class FullScreenAds(
    private val placement: BaseAdLoader<NativeAd>,
    private val delayBeforeSkip: Long = DEFAULT_DELAY_BEFORE_SKIP,
    @LayoutRes val layout: Int = R.layout.fullscreen_view
) {
    companion object {
        const val DEFAULT_DELAY_BEFORE_SKIP = 3000L
        val DEFAULT_TIMEOUT_FOR_AD_CALL = 6.seconds
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    suspend fun preload(
        fragmentActivity: FragmentActivity,
        listener: NativeAdLoaderListener
    ): FullscreenAdPresenter? {
        val preloaded = placement.awaitLoadedWithTimeout(
            activity = fragmentActivity,
            listener = listener,
            timeout = DEFAULT_TIMEOUT_FOR_AD_CALL
        )
        return if (preloaded != null) FullscreenAdPresenter(
            nativeAd = preloaded,
            fragmentActivity = fragmentActivity,
            layoutRes = layout,
            delayBeforeSkip = delayBeforeSkip
        )
        else null
    }

    class FullscreenAdPresenter(
        private val nativeAd: NativeAd,
        val fragmentActivity: FragmentActivity,
        @LayoutRes val layoutRes: Int,
        val delayBeforeSkip: Long
    ) {
        companion object {
            const val TAG = "FullscreenAds"

            const val BACKSTACK_FRAGMENT_NAME = "FullscreenAds"
        }

        fun show(@IdRes fragmentContainer: Int, listener: () -> Unit) {
            if (fragmentActivity.supportFragmentManager.isDestroyed)
                return

            val managedFragment = FullscreenAdsFragment()
            managedFragment.delayBeforeSkip = delayBeforeSkip
            managedFragment.nativeAd = nativeAd
            managedFragment.layout = layoutRes

            managedFragment.setOnFinishedListener(listener)

            fragmentActivity.lifecycleScope.launch {
                delay(50) // make suspend call, so if coroutine suspends, the block below won't execute

                if (!fragmentActivity.supportFragmentManager.isStateSaved && !fragmentActivity.isFinishing && !fragmentActivity.isDestroyed) {
                    val view = fragmentActivity.findViewById<FragmentContainerView>(fragmentContainer)
                    fragmentActivity.supportFragmentManager.beginTransaction().apply {

                        add(fragmentContainer, managedFragment, "NativeFullscreenAds")
                        addToBackStack(BACKSTACK_FRAGMENT_NAME)
                        commit()
                    }
                }
            }
        }
    }
}