package com.google.ads.formats

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.ads.core.AdRevenueListenerFactory
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date
import kotlin.reflect.KClass


class AppOpenPlacement(
    private val activityClass: KClass<*>,
    private val application: Application,
    private val adUnitId: String,
    private val ilrdFactory: AdRevenueListenerFactory,
    val adCallback: AppOpenAdCallback
) : Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private var track: Boolean = false
    private var currentActivity: Activity? = null
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false



    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
    private var loadTime: Long = 0

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** Check if ad was loaded more than n hours ago. */
    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * TTL_HOURS
    }

    /** Check if ad exists and can be shown. */
    private fun isAdAvailable(): Boolean {
        // For time interval details, see: https://support.google.com/admob/answer/9341964
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo()
    }

    override fun onActivityStopped(p0: Activity) {}
    override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
    override fun onActivityDestroyed(p0: Activity) {}
    override fun onActivityPaused(p0: Activity) {
        if (!track) return
        if (!activityClass.isInstance(p0)) return
        if (!isAdAvailable() && !isLoadingAd) loadAd()
    }

    override fun onActivityResumed(activity: Activity) {
        if (!track) return
        if (!isShowingAd && activityClass.isInstance(activity)) {
            showAdIfAvailable(activity)
        }
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
    override fun onActivityStarted(activity: Activity) {
        // An ad activity is started when an ad is showing, which could be AdActivity class from Google
        // SDK or another activity class implemented by a third party mediation partner. Updating the
        // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
        // one that shows the ad.
        if (!isShowingAd && activityClass.isInstance(activity)) {
            currentActivity = activity
        }
    }

    fun loadAd() {
        if (isLoadingAd) return
        isLoadingAd = true
        Log.d(TAG,"Loading AppOpenAds.")
        AppOpenAd.load(
            application,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoadingAd = false
                    appOpenAd = ad

                    // Called when an app open ad has loaded.
                    Log.d(TAG, "App open ad loaded.")

                    loadTime = Date().time
                    setupFullscreenCallback(ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Called when an app open ad has failed to load.
                    Log.d(TAG, "App open ad failed to load with error: " + loadAdError.message)

                    isLoadingAd = false
                }
            },
        )
    }

    private fun setupFullscreenCallback(ad: AppOpenAd) {
        ad.onPaidEventListener = ilrdFactory.createListenerWith(adUnitId = adUnitId, adObject = ad)

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Called when full screen content is dismissed.
                    Log.d(TAG, "Ad dismissed fullscreen content.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    appOpenAd = null
                    isShowingAd = false

                    adCallback.onAdFinishedToShow()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when full screen content failed to show.
                    Log.d(TAG, adError.message)
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    appOpenAd = null
                    isShowingAd = false

                    adCallback.onAdFinishedToShow()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "The ad recorded an impression.")
                    adCallback.onAdIsShowing()
                }

                override fun onAdClicked() {
                    // Called when ad is clicked.
                    Log.d(TAG, "The ad was clicked.")
                }
            }
    }

    fun showAdIfAvailable(activity: Activity) {
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            Log.d(TAG, "The app open ad is already showing.")
            return
        }

        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (appOpenAd == null || !isAdAvailable()) {
            Log.d(TAG, "The app open ad is not ready yet.")
            adCallback.onAdFinishedToShow()
            return
        }

        isShowingAd = true
        appOpenAd?.show(activity)
    }

    fun stopTrackResume() {
        track = false
    }
    fun startTrackResume() {
        track = true
    }

    companion object {
        const val TAG = "AppOpenPlacement"
        const val TTL_HOURS = 4
    }
}

open class AppOpenAdCallback {
    fun onAdFinishedToShow() {}
    fun onAdIsShowing() {}
}
