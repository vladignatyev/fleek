package com.google.ads.core

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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.suspendCoroutine
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
    ): AdCLass? = withTimeoutOrNull(timeout) {
        return@withTimeoutOrNull awaitLoaded(activity, listener)
    }
}

abstract class AdChain<AdClass> : ArrayList<BaseAdLoader<AdClass>>()

open class NativeAdChain() : AdChain<NativeAd>() {
    companion object {
        open fun fromList(
            adUnitIds: List<String>,
            nativeAdOptions: NativeAdOptions = NativeAdOptions.Builder().setVideoOptions(VideoOptions.Builder().setStartMuted(false).build()).build()
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
) : BaseAdLoader<AdClass>(chain[0].adUnitId) {
    var lastSuccessfulCall: AdClass? = null
    var lastSuccessfulCallIndex: Int = -1

    @RequiresPermission(Manifest.permission.INTERNET)
    override suspend fun awaitLoaded(
        activity: FragmentActivity,
        listener: BaseAdLoaderListener<AdClass>
    ): AdClass? {
        chain.forEachIndexed { i, loader ->
            Log.d(TAG, "Loading call #$i from chain...")
            val ad = loader.awaitLoadedWithTimeout(
                activity = activity,
                listener = listener,
                timeout = individualCallTimeout
            )
            Log.d(TAG, loader.toString())
            if (ad != null) {
                Log.d(TAG, "Got an ad response!")
                lastSuccessfulCallIndex = i
                lastSuccessfulCall = ad
                return ad
            }
        }
        Log.d(TAG, "Chain loaded without response.")
        return null
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
        CoroutineScope(Dispatchers.IO).launch {
            val adLoader = AdLoader.Builder(activity, adUnitId).withNativeAdOptions(nativeAdOptions)
                .withAdListener(listener).forNativeAd { ad ->
                    if (activity.isFinishing) return@forNativeAd
                    activity.runOnUiThread {
                        listener.onAdLoaded(ad)
                    }
                }.build()

            listener.onLoading()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    override suspend fun awaitLoaded(
        activity: FragmentActivity, listener: BaseAdLoaderListener<NativeAd>
    ) = suspendCoroutine { continuation ->
        val mListener = object : NativeAdLoaderListener(listener.adLoader) {
            override fun onAdFailedToLoad(error: LoadAdError) {
                activity.runOnUiThread {
                    if (continuation.context.isActive) {
                        listener.onAdFailedToLoad(error)
                        continuation.resumeWith(Result.success(null))
                    } else {
                        listener.onTimeout()
                        continuation.resumeWith(Result.success(null))
                    }
                }
            }

            override fun onAdLoaded(ad: NativeAd) {
                activity.runOnUiThread {
                    if (continuation.context.isActive) {
                        listener.onAdLoaded(ad)
                        continuation.resumeWith(Result.success(ad))
                    } else {
                        listener.onTimeout()
                        continuation.resumeWith(Result.success(null))
                    }
                }
            }

            override fun onLoading() {
                activity.runOnUiThread {
                    if (continuation.context.isActive) {
                        listener.onLoading()
                    } else {
                        listener.onTimeout()
                    }
                }
            }
        }

        this@BasicNativeAdLoader.load(activity, mListener)
    }
}

