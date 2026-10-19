package com.myarulin.movies_test.sdk.internal

import android.app.Activity
import android.content.Context
import android.view.View

internal interface NativeAdHandle

internal interface AdProvider {
    fun initialize(context: Context)

    fun loadNative(
        context: Context,
        onLoaded: (NativeAdHandle) -> Unit,
        onFailed: (String) -> Unit,
    )

    fun createNativeView(context: Context, handle: NativeAdHandle): View

    fun destroyNative(handle: NativeAdHandle)

    fun loadInterstitial(
        context: Context,
        onLoaded: () -> Unit,
        onFailed: () -> Unit,
    )

    fun showInterstitial(
        activity: Activity,
        onDismissed: () -> Unit,
        onFailed: () -> Unit,
    ): Boolean

    fun release()
}
