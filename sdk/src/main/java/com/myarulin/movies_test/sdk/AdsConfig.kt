package com.myarulin.movies_test.sdk

data class AdsConfig(
    val nativeUnitId: String = DEMO_NATIVE_UNIT_ID,
    val interstitialUnitId: String = DEMO_INTERSTITIAL_UNIT_ID,
) {
    private companion object {
        const val DEMO_NATIVE_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
        const val DEMO_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    }
}
