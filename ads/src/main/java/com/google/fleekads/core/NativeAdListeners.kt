package com.google.fleekads.core

import com.google.android.gms.ads.nativead.NativeAd


open class AdListenerWithRevenue(
    adLoader: BaseAdLoader<NativeAd>,
    val ilrdFactory: AdRevenueListenerFactory
) : NativeAdLoaderListener(adLoader) {
    init {
        if (adLoader.adUnitId.isEmpty()) {
            throw IllegalStateException("Ad unit id shouldn't be empty.")
        }
    }

    override fun onAdLoaded(ad: NativeAd) {
        ad.setOnPaidEventListener(
            ilrdFactory.createListenerWith(
                adLoader.adUnitId, ad
            )
        )
        super.onAdLoaded(ad)
    }
}

class AdListenerWithAdFieldAndRevenue(
    adLoader: BaseAdLoader<NativeAd>,
    adField: AdField.Writer<NativeAd>,
    ilrdFactory: AdRevenueListenerFactory
) : ChainedAdListener<NativeAd>(
    listOf(
        AdListenerWithAdField(adLoader, adField),
        AdListenerWithRevenue(adLoader, ilrdFactory)
    )
)



