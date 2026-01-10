package com.google.fleekads.attribution

import android.util.Log
import com.google.fleekads.attribution.TenjinAttributionWithAdRevenue.Companion.ILRD_TAG
import com.google.fleekads.core.AdRevenueListenerFactory
import com.google.android.gms.ads.OnPaidEventListener

class DummyAttribution : AdRevenueListenerFactory() {
    override fun createListenerWith(adUnitId: String, adObject: Any): OnPaidEventListener {
        Log.d(ILRD_TAG, "Creating ILRD listener for `${adUnitId}`")

        if (adUnitId.isEmpty()) {
            throw IllegalStateException("adUnitId shouldn't be empty! ${adObject.toString()}")
        }

        return OnPaidEventListener { adValue ->
            Log.d(
                ILRD_TAG,
                "Ad Revenue Reported:\n\tAd Unit: $adUnitId\n\tValue:${adValue.valueMicros}\n\tCurrency:${adValue.currencyCode}\n\tPrecision:${adValue.precisionType}"
            )
        }
    }
}