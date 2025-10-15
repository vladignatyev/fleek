package com.google.ads.formats

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.ads.nativead.NativeAd
import com.google.ads.core.AdField
import com.google.ads.core.AdListenerWithAdFieldAndRevenue
import com.google.ads.core.AdRevenueListenerFactory
import com.google.ads.core.BaseAdLoader
import com.google.ads.core.BaseAdLoaderListener

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
