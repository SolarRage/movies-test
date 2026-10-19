package com.myarulin.movies_test.sdk

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface AdsManager {
    fun initialize(application: Application)

    @Composable
    fun NativeAd(slotId: String, modifier: Modifier = Modifier)

    fun release()
}
