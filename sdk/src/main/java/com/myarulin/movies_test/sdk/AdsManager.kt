package com.myarulin.movies_test.sdk

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class NativeAdsLoadState {
    Loading,
    Ready,
    Failed,
}

interface AdsManager {
    fun initialize(application: Application)

    @Composable
    fun prepareNativeAds(slotIds: Set<String>): Map<String, NativeAdsLoadState>

    @Composable
    fun NativeAd(slotId: String, modifier: Modifier)

    fun release()
}
