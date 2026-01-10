package com.google.fleekads.formats

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.ads.nativead.NativeAd
import com.google.fleekads.core.AdField
import com.google.fleekads.core.AdListenerWithAdFieldAndRevenue
import com.google.fleekads.core.AdRevenueListenerFactory
import com.google.fleekads.core.BaseAdLoader
import com.google.fleekads.core.BaseAdLoaderListener

class BasicNativePlacement(
    val loader: BaseAdLoader<NativeAd>,
) : BaseAdLoader<NativeAd>(loader.adUnitId) {
    var ad: AdField<NativeAd> = AdField()

    @RequiresPermission(Manifest.permission.INTERNET)
    fun load(activity: FragmentActivity, ilrdEventListenerFactory: AdRevenueListenerFactory) {
        return loader.load(
            activity, AdListenerWithAdFieldAndRevenue(
                loader, ad.forWrite(), ilrdEventListenerFactory
            )
        )
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    override suspend fun awaitLoaded(
        activity: FragmentActivity,
        listener: BaseAdLoaderListener<NativeAd>
    ): NativeAd? =
        loader.awaitLoaded(
            activity = activity,
            listener = listener
        )

}
