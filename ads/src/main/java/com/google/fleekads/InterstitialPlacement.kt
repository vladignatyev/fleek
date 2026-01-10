package com.google.fleekads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.fleekads.core.AdRevenueListenerFactory
import java.util.Date


interface OnAdDismissed {
    fun onAdClosed()
}


class InterstitialPlacement(
    val adUnitId: String,
    val ilrdFactory: AdRevenueListenerFactory
) {
    var returnBackInter: InterstitialAd? = null
    var returnBackInterLoading = false
    var loadTime = 0L

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    fun load(activity: Activity) {
        if (returnBackInterLoading) {
            // Already loading.
            return
        }

        if (returnBackInter != null && wasLoadTimeLessThanNHoursAgo(4)) {
            // Already have fresh ads.
            return
        }

        returnBackInterLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd.onPaidEventListener =
                        ilrdFactory.createListenerWith(
                            adUnitId,
                            interstitialAd
                        )
                    returnBackInter = interstitialAd
                    returnBackInterLoading = false
                    loadTime = Date().time
                }
            })
    }


    fun present(activity: Activity, onAdClosed: OnAdDismissed) {
        if (returnBackInter != null) {

            returnBackInter?.let {
                it.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        onAdClosed.onAdClosed()
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        onAdClosed.onAdClosed()
                    }
                }

                it.show(activity)
            }

            returnBackInterLoading = false
            returnBackInter = null
        } else {
            onAdClosed.onAdClosed()
        }

        load(activity)
    }
}
