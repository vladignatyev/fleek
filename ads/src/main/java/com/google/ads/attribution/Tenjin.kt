package com.google.ads.attribution

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.OnPaidEventListener
import com.google.ads.core.AdRevenueListenerFactory
import com.tenjin.android.TenjinSDK
import org.json.JSONObject


class TenjinAttributionWithAdRevenue(
    private val activity: Context, private val sdkKey: String
) : AdRevenueListenerFactory() {
    private var sdkInstance: TenjinSDK = TenjinSDK.getInstance(activity, sdkKey)!!
        get() = field

    override fun createListenerWith(adUnitId: String, adObject: Any): OnPaidEventListener {
        Log.d(ILRD_TAG, "Creating ILRD listener for `${adUnitId}`")

        if (adUnitId.isEmpty()) {
            throw IllegalStateException("adUnitId shouldn't be empty! ${adObject.toString()}")
        }

        return OnPaidEventListener { adValue ->
            // Get the ad unit ID
            val adUnitId = adUnitId

            // Extract the impression-level ad revenue data
            val valueMicros = adValue.valueMicros
            val currencyCode = adValue.currencyCode
            val precisionType = adValue.precisionType

            val json = JSONObject()
            json.put("ad_unit_id", adUnitId)
            json.put("currency_code", currencyCode)
            json.put("value_micros", valueMicros)
            json.put("precision_type", precisionType)

            sdkInstance.eventAdImpressionAdMob(json)

            Log.d(
                ILRD_TAG,
                "Ad Revenue Reported:\n\tAd Unit: $adUnitId\n\tValue:${adValue.valueMicros}\n\tCurrency:${adValue.currencyCode}\n\tPrecision:${adValue.precisionType}"
            )
        }
    }

    fun provideGdpr() {
        if (sdkInstance.optInOutUsingCMP()) {
            sdkInstance.optIn()
        } else {
            sdkInstance.optOut()
        }
    }

    /**
     * As stated at https://docs.tenjin.com/docs/android-sdk#app-initialization the method `connect()`
     * should be called on every onResume(), not just the first one that happens on the app initialization.
     * */
    fun connectOnResume() {
        sdkInstance.connect()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: TenjinAttributionWithAdRevenue? = null

        fun create(activity: Activity, sdkKey: String): TenjinAttributionWithAdRevenue {
            if (instance == null) {
                instance = TenjinAttributionWithAdRevenue(activity, sdkKey)

                instance!!.sdkInstance.setAppStore(TenjinSDK.AppStoreType.googleplay)
            }
            return instance!!
        }

        fun getInstance(): TenjinAttributionWithAdRevenue {
            if (instance == null) {
                throw IllegalStateException(
                    "You should create TenjinAttributionWithAdRevenue instance " +
                            "in onStart() of your Activity before calling super onStart(savedInstanceState)."
                )
            }
            return instance!!
        }

        const val ILRD_TAG = "ILRD"
    }
}
