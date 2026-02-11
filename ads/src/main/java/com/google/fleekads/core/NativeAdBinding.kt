package com.google.fleekads.core

import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.fleekads.R

class NativeAdsBinding(val rootView: View) {
    var priceView: TextView? = null
    var mediaView: MediaView? = null
    var iconView: ImageView? = null
    var callToActionView: Button? = null
    var ratingValue: TextView? = null
    var ratingBar: RatingBar? = null
    var adAttribution: View? = null
    var bodyView: TextView? = null
    var advertiserOrStoreView: TextView? = null
    var titleView: TextView? = null
    var nativeAdView: NativeAdView? = null

    private var _populateWithAd: NativeAd? = null
        get() = field

    init {
        bind()
    }

    private fun bind() {
        rootView.apply {
            nativeAdView = findViewById(R.id.native_ad_view)
            titleView = findViewById(R.id.ad_title)
            advertiserOrStoreView =
                findViewById(R.id.ad_secondary)
            bodyView = findViewById(R.id.ad_body)
            adAttribution = findViewById(R.id.ad_attribution)
            ratingValue = findViewById(R.id.ad_rating_value)
            ratingBar = findViewById(R.id.ad_rating_bar)
            callToActionView = findViewById(R.id.ad_cta)
            iconView = findViewById(R.id.ad_icon)
            mediaView = findViewById(R.id.ad_media)
            priceView = findViewById(R.id.ad_price)
        }
    }

    fun populate(nativeAd: NativeAd) {
        val store = nativeAd.store
        val advertiser = nativeAd.advertiser
        val headline = nativeAd.headline
        val body = nativeAd.body
        val cta = nativeAd.callToAction
        val starRating = nativeAd.starRating
        val icon = nativeAd.icon
        val price = nativeAd.price

        titleView!!.text = headline
        nativeAdView!!.headlineView = titleView

        if (mediaView != null) {
            nativeAdView!!.mediaView = mediaView
        }

        if (advertiserOrStoreView != null) {
            if (!TextUtils.isEmpty(advertiser)) {
                advertiserOrStoreView!!.visibility = View.VISIBLE
                advertiserOrStoreView!!.text = advertiser

                nativeAdView!!.advertiserView = advertiserOrStoreView
            } else if (!TextUtils.isEmpty(store)) {
                advertiserOrStoreView!!.visibility = View.VISIBLE
                advertiserOrStoreView!!.text = store

                nativeAdView!!.storeView = advertiserOrStoreView
            } else {
                advertiserOrStoreView!!.visibility = View.GONE
            }
        }

        //  Set the secondary view to be the star rating if available.
        if (starRating != null) {
            if (ratingValue != null) {
                if (starRating > 0) {
                    ratingValue!!.visibility = View.VISIBLE
                    ratingValue!!.text = String.format("★ %.1f", starRating.toFloat())
                    nativeAdView!!.starRatingView = ratingValue
                } else {
                    ratingValue!!.visibility = View.GONE
                }
            } else if (ratingBar != null) {
                if (starRating > 0) {
                    ratingBar!!.visibility = View.VISIBLE
                    ratingBar!!.rating = starRating.toFloat()
                    nativeAdView!!.starRatingView = ratingBar
                } else {
                    ratingBar!!.visibility = View.GONE
                }
            }
        }

        if (priceView != null) {
            if (price != null) {
                priceView!!.visibility = View.VISIBLE
                priceView!!.text = price
                nativeAdView!!.priceView = priceView
            } else {
                priceView!!.visibility = View.GONE
            }
        }

        if (iconView != null) {
            if (icon != null) {
                iconView!!.visibility = View.VISIBLE
                iconView!!.setImageDrawable(icon.drawable)
                nativeAdView!!.iconView = iconView
            } else {
                iconView!!.visibility = View.GONE
            }
        }

        if (bodyView != null) {
            if (body != null) {
                bodyView!!.text = body
                bodyView!!.visibility = View.VISIBLE
                nativeAdView!!.bodyView = bodyView
            } else {
                bodyView!!.visibility = View.GONE
            }
        }

        if (adAttribution != null) {
            adAttribution!!.visibility = View.VISIBLE
        }

        if (callToActionView != null) {
            if (cta != null) {
                callToActionView!!.visibility = View.VISIBLE
                callToActionView!!.text = cta
                nativeAdView!!.callToActionView = callToActionView
            } else {
                callToActionView!!.visibility = View.GONE
            }
        }

        nativeAdView!!.setNativeAd(nativeAd)

        _populateWithAd = nativeAd
    }

    fun destroy() {
        val ad = _populateWithAd
        ad?.destroy()
        _populateWithAd = null
    }
}
