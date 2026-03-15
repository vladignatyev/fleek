package com.google.fleekads.core

import android.Manifest
import android.app.Activity
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


open class NativeAdLoaderListener(adLoader: BaseAdLoader<NativeAd>) :
    BaseAdLoaderListener<NativeAd>(adLoader)


open class BaseAdLoader<AdCLass>(reportingAdUnitId: String) {
    init {
        if (reportingAdUnitId.isEmpty()) {
            throw IllegalStateException("reportingAdUnitId shouldn't be empty.")
        }
    }

    open val adUnitId: String = reportingAdUnitId

    open fun load(activity: Activity, listener: BaseAdLoaderListener<AdCLass>) {
        throw NotImplementedError("Should implement load()")
    }

    open suspend fun awaitLoaded(
        activity: FragmentActivity, listener: BaseAdLoaderListener<AdCLass>
    ): AdCLass? {
        throw NotImplementedError("Should implement awaitLoaded()")
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    suspend fun awaitLoadedWithTimeout(
        activity: FragmentActivity,
        listener: BaseAdLoaderListener<AdCLass>,
        timeout: Duration,
    ): AdCLass? {
        val result = withTimeoutOrNull(timeout) {
            return@withTimeoutOrNull awaitLoaded(activity, listener)
        }
        if (result == null) {
            listener.onTimeout()
            return result
        } else {
            return result
        }
    }
}

abstract class AdChain<AdClass> : ArrayList<BaseAdLoader<AdClass>>()

open class NativeAdChain() : AdChain<NativeAd>() {
    companion object {
        open fun fromList(
            adUnitIds: List<String>,
            nativeAdOptions: NativeAdOptions = NativeAdOptions.Builder()
                .setVideoOptions(VideoOptions.Builder().setStartMuted(false).build()).build()
        ): NativeAdChain {
            val result = NativeAdChain()
            adUnitIds.forEach { adUnitId ->
                result.add(BasicNativeAdLoader(adUnitId, nativeAdOptions))
            }
            return result
        }
    }
}


val DEFAULT_INDIVIDUAL_AD_CALL_TIMEOUT = 5.seconds

open class ChainAdLoader<AdClass>(
    open val chain: AdChain<AdClass>,
    open val individualCallTimeout: Duration = DEFAULT_INDIVIDUAL_AD_CALL_TIMEOUT
) : BaseAdLoader<AdClass>(chain.firstOrNull()?.adUnitId ?: "chain_placeholder") {
    init {
        require(chain.isNotEmpty()) { "Chain should contain at least one ad loader." }
    }

    var lastSuccessfulCall: AdClass? = null
    var lastSuccessfulCallIndex: Int = -1

    @RequiresPermission(Manifest.permission.INTERNET)
    override suspend fun awaitLoaded(
        activity: FragmentActivity,
        listener: BaseAdLoaderListener<AdClass>
    ): AdClass? {
        chain.forEachIndexed { i, loader ->
            Log.d(TAG, "Loading call #$i from chain...")
            val chainListener = ChainStepListener(listener, loader)
            val ad = loader.awaitLoadedWithTimeout(
                activity = activity,
                listener = chainListener,
                timeout = individualCallTimeout
            )

            if (ad != null) {
                Log.d(TAG, "Got an ad response!")
                lastSuccessfulCallIndex = i
                lastSuccessfulCall = ad
                return ad
            }

            if (chainListener.receivedNoFill) {
                Log.d(TAG, "No fill from call #$i, stopping chain as requested.")
                return null
            }
        }
        Log.d(TAG, "Chain loaded without response.")
        return null
    }

    private class ChainStepListener<AdClass>(
        private val delegate: BaseAdLoaderListener<AdClass>,
        override val adLoader: BaseAdLoader<AdClass>
    ) : BaseAdLoaderListener<AdClass>(adLoader) {
        var receivedNoFill: Boolean = false
            private set

        override fun onAdLoaded(ad: AdClass) {
            delegate.onAdLoaded(ad)
        }

        override fun onLoading() {
            delegate.onLoading()
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            delegate.onAdFailedToLoad(error)
            if (error.code == AdRequest.ERROR_CODE_NO_FILL) {
                receivedNoFill = true
            }
        }

        override fun onAdClicked() {
            delegate.onAdClicked()
        }

        override fun onTimeout() {
            delegate.onTimeout()
        }
    }

    companion object {
        const val TAG = "ChainAdLoader"
    }
}

open class ChainNativeAdLoader(
    chain: NativeAdChain,
    individualCallTimeout: Duration = DEFAULT_INDIVIDUAL_AD_CALL_TIMEOUT
) :
    ChainAdLoader<NativeAd>(chain, individualCallTimeout)

open class BasicNativeAdLoader(
    adUnitId: String,
    open val nativeAdOptions: NativeAdOptions = NativeAdOptions.Builder()
        .setVideoOptions(VideoOptions.Builder().setStartMuted(false).build()).build(),
) : BaseAdLoader<NativeAd>(adUnitId) {
    @RequiresPermission(Manifest.permission.INTERNET)
    override fun load(activity: Activity, listener: BaseAdLoaderListener<NativeAd>) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            if (activity.isFinishing || activity.isDestroyed) {
                listener.onAdFailedToLoad(
                    LoadAdError(0, "Activity is finishing/destroyed.", "", null, null)
                )
                return@launch
            }

            val adLoader = AdLoader.Builder(activity, adUnitId)
                .withNativeAdOptions(nativeAdOptions)
                .withAdListener(listener)
                .forNativeAd { ad ->
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        listener.onAdLoaded(ad)
                    } else {
                        ad.destroy()
                    }
                }
                .build()

            listener.onLoading()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    override suspend fun awaitLoaded(
        activity: FragmentActivity, listener: BaseAdLoaderListener<NativeAd>
    ): NativeAd? = suspendCancellableCoroutine { continuation ->
        val isResolved = AtomicBoolean(false)

        fun tryResumeWith(value: NativeAd?) {
            if (isResolved.compareAndSet(false, true) && continuation.isActive) {
                continuation.resume(value) {}
            } else if (value != null) {
                value.destroy()
            }
        }

        val mListener = object : NativeAdLoaderListener(listener.adLoader) {
            override fun onAdFailedToLoad(error: LoadAdError) {
                if (!isResolved.get()) {
                    listener.onAdFailedToLoad(error)
                }
                tryResumeWith(null)
            }

            override fun onAdLoaded(ad: NativeAd) {
                if (!isResolved.get()) {
                    listener.onAdLoaded(ad)
                }
                tryResumeWith(ad)
            }

            override fun onLoading() {
                if (!isResolved.get()) {
                    listener.onLoading()
                }
            }
        }

        continuation.invokeOnCancellation {
            isResolved.set(true)
        }

        this@BasicNativeAdLoader.load(activity, mListener)
    }
}
