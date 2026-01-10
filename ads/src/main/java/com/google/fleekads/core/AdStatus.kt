package com.google.fleekads.core

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError

sealed class AdStatus<AdClass>(val whenTime: Long = getCurrentTimestamp()) {
    companion object {
        fun getCurrentTimestamp(): Long {
            return System.currentTimeMillis()
        }
    }

    class NotReady<AdClass> : AdStatus<AdClass>()
    class Loading<AdClass> : AdStatus<AdClass>()
    class Failed<AdClass> : AdStatus<AdClass>()
    data class Ready<AdClass>(val ad: AdClass) : AdStatus<AdClass>()
}

open class BaseAdLoaderListener<AdClass>(open val adLoader: BaseAdLoader<AdClass>) : AdListener() {
    open fun onAdLoaded(ad: AdClass) {}
    open fun onLoading() {}

    override fun onAdClicked() {
        super.onAdClicked()
    }

    open fun onTimeout() {
        onAdFailedToLoad(LoadAdError(0, "Timeout.", "", null, null))
    }
}

open class ChainedAdListener<AdClass>(private val listeners: List<BaseAdLoaderListener<AdClass>?>) :
    BaseAdLoaderListener<AdClass>(listeners[0]!!.adLoader) {

    override fun onAdFailedToLoad(error: LoadAdError) =
        listeners.forEach { it?.onAdFailedToLoad(error) }

    override fun onAdLoaded(ad: AdClass) = listeners.forEach { it?.onAdLoaded(ad) }
    override fun onLoading() = listeners.forEach { it?.onLoading() }
    override fun onAdClicked() = listeners.forEach { it?.onAdClicked() }
}


class AdListenerWithAdField<AdClass>(
    override val adLoader: BaseAdLoader<AdClass>,
    val adField: AdField.Writer<AdClass>
) : BaseAdLoaderListener<AdClass>(adLoader) {
    override fun onAdFailedToLoad(error: LoadAdError) {
        adField.apply {
            setStatus(AdStatus.Failed())
            finish()
        }
    }

    override fun onAdLoaded(ad: AdClass) {
        adField.apply {
            setStatus(AdStatus.Ready(ad))
            finish()
        }
    }

    override fun onLoading() {
        adField.apply {
            setStatus(AdStatus.Loading())
        }
    }
}
