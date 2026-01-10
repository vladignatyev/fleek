package com.google.fleekads.core

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AdField<AdClass> {
    private val _status: MutableLiveData<AdStatus<AdClass>> = MutableLiveData(AdStatus.NotReady())
    val status: LiveData<AdStatus<AdClass>> = _status

    class Writer<AdClass>(
        private val status: MutableLiveData<AdStatus<AdClass>>
    ) {
        private var finalized: Boolean = false

        fun setStatus(adStatus: AdStatus<AdClass>): Writer<AdClass> {
            if (finalized)
                throw IllegalStateException("setStatus() unable to set as the AdField.Writer has already been finalized.")

            status.value = adStatus
            return this
        }

        fun finish() {
            if (finalized)
                throw IllegalStateException("AdField.Writer has already been finalized.")

            finalized = true
        }
    }

    fun forWrite(): Writer<AdClass> = Writer(_status)

    fun getTtlMilliseconds(): Long? = _status.value.let { wasReady ->
        if (wasReady is AdStatus.Ready) {
            AdStatus.getCurrentTimestamp() - wasReady.whenTime
        } else {
            null
        }
    }
}