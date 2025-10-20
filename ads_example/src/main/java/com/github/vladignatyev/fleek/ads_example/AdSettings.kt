package com.github.vladignatyev.fleek.ads_example

import com.google.ads.attribution.DummyAttribution

/**
 * Declaring ad unit IDs mapping.
 * In a real project they may be obtained from a remote config or defined in separate module/object.
 * */
const val appOpenAID = "ca-app-pub-3940256099942544/9257395921"
const val nativeAID = "ca-app-pub-3940256099942544/2247696110"
const val interAID = "ca-app-pub-3940256099942544/1033173712"

val nativeChainAIDs = listOf(
    "ca-app-pub-3940256099942544/2247696110", // high ecpm
    "ca-app-pub-3940256099942544/2247696110", // medium ecpm
    "ca-app-pub-3940256099942544/2247696110", // low ecpm
)

// Define the attribution/ILRD handler provider.
// Please note, that depending on provider, it may require initialization
// when onCreate/onResume events happen in Activity or Fragment.
var attribution: DummyAttribution = DummyAttribution()

// Uncomment following strings if you plan to use Tenjin as an attribution provider instead:
//    private lateinit var attribution: TenjinAttributionWithAdRevenue
