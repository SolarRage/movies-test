package com.myarulin.movies_test.sdk.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.myarulin.movies_test.sdk.AdsManager
import java.lang.ref.WeakReference

internal class DefaultAdsManager(
    private val provider: AdProvider,
) : AdsManager, DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    private sealed interface NativeSlotState {
        data object Loading : NativeSlotState
        data class Loaded(val handle: NativeAdHandle) : NativeSlotState
        data object Failed : NativeSlotState
    }

    private val nativeSlots = mutableStateMapOf<String, NativeSlotState>()
    private var application: Application? = null
    private var policy: InterstitialPolicy? = null
    private var currentActivity = WeakReference<Activity>(null)
    private var pendingInterstitial = false
    private var interstitialReady = false
    private var interstitialShowing = false

    override fun initialize(application: Application) {
        if (this.application != null) return
        this.application = application
        policy = InterstitialPolicy(
            SharedPreferencesSessionStore(
                application.getSharedPreferences(PREFERENCES_NAME, Application.MODE_PRIVATE),
            ),
        )
        provider.initialize(application)
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        preloadInterstitial()
    }

    override fun onStart(owner: LifecycleOwner) {
        pendingInterstitial = policy?.onForeground() == true
        if (pendingInterstitial) tryShowInterstitial()
    }

    @Composable
    override fun NativeAd(slotId: String, modifier: Modifier) {
        LaunchedEffect(slotId) {
            loadNative(slotId)
        }
        DisposableEffect(slotId) {
            onDispose { releaseNative(slotId) }
        }

        when (val state = nativeSlots[slotId]) {
            NativeSlotState.Loading -> LinearProgressIndicator(modifier = modifier.fillMaxWidth())
            is NativeSlotState.Loaded -> key(state.handle) {
                AndroidView(
                    factory = { context -> provider.createNativeView(context, state.handle) },
                    modifier = modifier.fillMaxWidth(),
                )
            }
            NativeSlotState.Failed, null -> Unit
        }
    }

    override fun release() {
        nativeSlots.keys.toList().forEach(::releaseNative)
        application?.unregisterActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        provider.release()
        application = null
        currentActivity.clear()
    }

    private fun loadNative(slotId: String) {
        if (nativeSlots[slotId] is NativeSlotState.Loaded || nativeSlots[slotId] is NativeSlotState.Loading) {
            return
        }
        val context = application ?: return
        nativeSlots[slotId] = NativeSlotState.Loading
        provider.loadNative(
            context = context,
            onLoaded = { handle ->
                (nativeSlots.put(slotId, NativeSlotState.Loaded(handle)) as? NativeSlotState.Loaded)
                    ?.handle
                    ?.let(provider::destroyNative)
            },
            onFailed = { nativeSlots[slotId] = NativeSlotState.Failed },
        )
    }

    private fun releaseNative(slotId: String) {
        (nativeSlots.remove(slotId) as? NativeSlotState.Loaded)
            ?.handle
            ?.let(provider::destroyNative)
    }

    private fun preloadInterstitial() {
        val context = application ?: return
        provider.loadInterstitial(
            context = context,
            onLoaded = {
                interstitialReady = true
                if (pendingInterstitial) tryShowInterstitial()
            },
            onFailed = { interstitialReady = false },
        )
    }

    private fun tryShowInterstitial() {
        if (!pendingInterstitial || !interstitialReady || interstitialShowing) return
        val activity = currentActivity.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return

        val started = provider.showInterstitial(
            activity = activity,
            onDismissed = {
                interstitialShowing = false
                interstitialReady = false
                pendingInterstitial = false
                policy?.markShown()
            },
            onFailed = {
                interstitialShowing = false
                interstitialReady = false
                preloadInterstitial()
            },
        )
        interstitialShowing = started
        if (!started) preloadInterstitial()
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
        if (pendingInterstitial) tryShowInterstitial()
    }

    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity.get() === activity) currentActivity.clear()
    }

    private companion object {
        const val PREFERENCES_NAME = "movies_ads_sdk_state"
    }
}
