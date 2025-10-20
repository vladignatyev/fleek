package com.google.ads.core

import com.google.android.gms.ads.OnPaidEventListener

open class AdRevenueListenerFactory {
    open fun createListenerWith(adUnitId: String, adObject: Any): OnPaidEventListener {
        return OnPaidEventListener { }
    }
}