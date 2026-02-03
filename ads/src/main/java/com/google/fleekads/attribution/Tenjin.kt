package com.google.fleekads.attribution

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.OnPaidEventListener
import com.google.fleekads.core.AdRevenueListenerFactory
import com.tenjin.android.TenjinSDK
import org.json.JSONObject


data class PaidEvent(
    val adUnitId: String,
    val adValueMicros: Long,
    val currencyCode: String,
    val precisionType: Int
) {
    fun toJSON(): JSONObject {
        val json = JSONObject()
        json.put("ad_unit_id", adUnitId)
        json.put("currency_code", currencyCode)
        json.put("value_micros", adValueMicros)
        json.put("precision_type", precisionType)
        return json
    }

    companion object {
        fun fromAdValue(adUnitId: String, adValue: AdValue): PaidEvent {
            // Extract the impression-level ad revenue data
            val valueMicros = adValue.valueMicros
            val currencyCode = adValue.currencyCode
            val precisionType = adValue.precisionType

            return PaidEvent(
                adUnitId = adUnitId,
                adValueMicros = valueMicros,
                currencyCode = currencyCode,
                precisionType = precisionType,
            )
        }
    }
}

// TODO: Make a general class independent of Tenjin
class TenjinAttributionWithAdRevenue(
    private val activity: Context, private val sdkKey: String
) : AdRevenueListenerFactory() {
    private var sdkInstance: TenjinSDK = TenjinSDK.getInstance(activity, sdkKey)!!
        get() = field

    var reportImmediately: Boolean = false

    var reportPrecise: Boolean = true
    var reportEstimated: Boolean = false
    var reportPublisherProvided: Boolean = false
    var reportUnknown: Boolean = false
    var reportZero: Boolean = false

    override fun createListenerWith(adUnitId: String, adObject: Any): OnPaidEventListener {
        Log.d(ILRD_TAG, "Creating ILRD listener for `${adUnitId}`")

        if (adUnitId.isEmpty()) {
            throw IllegalStateException("adUnitId shouldn't be empty! ${adObject.toString()}")
        }

        return createDeferredPaidEventListener(adUnitId)
    }

    val deferredPaidEvents = ArrayList<PaidEvent>()

    private fun createDeferredPaidEventListener(adUnitId: String): OnPaidEventListener =
        OnPaidEventListener { adValue ->
            if (reportImmediately) {
                reportAdRevenue(event = PaidEvent.fromAdValue(adUnitId, adValue))
            } else {
                storeAdRevenue(event = PaidEvent.fromAdValue(adUnitId, adValue))
            }
        }

    private fun reportAdRevenue(event: PaidEvent) {
        sdkInstance.eventAdImpressionAdMob(event.toJSON())

        Log.d(
            ILRD_TAG,
            "Ad Revenue Reported:\n\tAd Unit: $event.adUnitId\n\tValue:${event.adValueMicros}\n\tCurrency:${event.currencyCode}\n\tPrecision:${event.precisionType}"
        )
    }

    private fun storeAdRevenue(event: PaidEvent) {
        deferredPaidEvents.add(event)
    }

    val predicate: (PaidEvent) -> Boolean = { event ->
        (reportZero || event.adValueMicros > 0) && (
                (reportPrecise && event.precisionType == AdValue.PrecisionType.PRECISE)
                        || (reportEstimated && event.precisionType == AdValue.PrecisionType.ESTIMATED)
                        || (reportPublisherProvided && event.precisionType == AdValue.PrecisionType.PUBLISHER_PROVIDED)
                        || (reportUnknown && event.precisionType == AdValue.PrecisionType.UNKNOWN)
                )
    }

    fun reportDefferedPaidEvents(): List<PaidEvent> {
        Log.d(
            ILRD_TAG,
            "*** Reporting deferred paid events. Totally ${deferredPaidEvents.count()} events caught.***"
        )

        Log.d(
            ILRD_TAG, "${deferredPaidEvents.count(predicate)} events will be reported."
        )

        val toBeReported = ArrayList<PaidEvent>(deferredPaidEvents.filter(predicate))

        toBeReported.map { event ->
            reportAdRevenue(event)
        }

        deferredPaidEvents.clear()

        Log.d(
            ILRD_TAG, "*** Deferred events reported and queue has been cleared ***"
        )

        return toBeReported
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
                    "You should create TenjinAttributionWithAdRevenue instance " + "in onStart() of your Activity before calling super onStart(savedInstanceState)."
                )
            }
            return instance!!
        }

        const val ILRD_TAG = "ILRD"
    }
}

