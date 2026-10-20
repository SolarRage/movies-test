package com.myarulin.movies_test.sdk.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.myarulin.movies_test.sdk.AdsManager
import java.lang.ref.WeakReference

internal class DefaultAdsManager(
    private val provider: AdProvider,
    private val sessionStoreFactory: (Application) -> InterstitialSessionStore = { application ->
        SharedPreferencesSessionStore(
            application.getSharedPreferences(PREFERENCES_NAME, Application.MODE_PRIVATE),
        )
    },
    private val processLifecycle: Lifecycle = ProcessLifecycleOwner.get().lifecycle,
) : AdsManager, DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    private sealed interface NativeSlotState {
        data class Loading(val requestId: Long) : NativeSlotState
        data class Loaded(val handle: NativeAdHandle) : NativeSlotState
        data object Failed : NativeSlotState
    }

    private enum class InterstitialState {
        Idle,
        Loading,
        Ready,
        Showing,
    }

    private val nativeSlots = mutableStateMapOf<String, NativeSlotState>()
    private val retryHandler = Handler(Looper.getMainLooper())
    private var application: Application? = null
    private var policy: InterstitialPolicy? = null
    private var currentActivity = WeakReference<Activity>(null)
    private var nextNativeRequestId = 0L
    private var interstitialRequestId = 0L
    private var interstitialState = InterstitialState.Idle
    private var eligibleLoadAttempts = 0
    private var retryScheduled = false
    private var processInForeground = false
    private var pendingInterstitial = false
    private val retryInterstitial = Runnable {
        retryScheduled = false
        if (canRetryInterstitial()) {
            loadInterstitial(countAsEligibleAttempt = true)
        }
    }

    override fun initialize(application: Application) {
        if (this.application != null) return
        this.application = application
        policy = InterstitialPolicy(sessionStoreFactory(application))
        provider.initialize(application)
        application.registerActivityLifecycleCallbacks(this)
        processLifecycle.addObserver(this)
        loadInterstitial(countAsEligibleAttempt = false)
    }

    override fun onStart(owner: LifecycleOwner) {
        processInForeground = true
        pendingInterstitial = policy?.onForeground() == true
        eligibleLoadAttempts = 0
        cancelInterstitialRetry()
        if (pendingInterstitial) {
            tryShowInterstitial()
            if (interstitialState == InterstitialState.Idle) {
                loadInterstitial(countAsEligibleAttempt = true)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        processInForeground = false
        pendingInterstitial = false
        eligibleLoadAttempts = 0
        cancelInterstitialRetry()
        currentActivity.clear()
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
            is NativeSlotState.Loading -> LinearProgressIndicator(modifier = modifier.fillMaxWidth())
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
        cancelInterstitialRetry()
        nativeSlots.keys.toList().forEach(::releaseNative)
        application?.unregisterActivityLifecycleCallbacks(this)
        processLifecycle.removeObserver(this)
        interstitialRequestId += 1
        interstitialState = InterstitialState.Idle
        eligibleLoadAttempts = 0
        processInForeground = false
        pendingInterstitial = false
        provider.release()
        application = null
        currentActivity.clear()
    }

    internal fun loadNative(slotId: String) {
        if (nativeSlots[slotId] is NativeSlotState.Loaded || nativeSlots[slotId] is NativeSlotState.Loading) {
            return
        }
        val context = application ?: return
        val requestId = ++nextNativeRequestId
        nativeSlots[slotId] = NativeSlotState.Loading(requestId)
        provider.loadNative(
            context = context,
            onLoaded = { handle ->
                val activeRequest = nativeSlots[slotId] as? NativeSlotState.Loading
                if (activeRequest?.requestId == requestId && application != null) {
                    nativeSlots[slotId] = NativeSlotState.Loaded(handle)
                } else {
                    provider.destroyNative(handle)
                }
            },
            onFailed = {
                val activeRequest = nativeSlots[slotId] as? NativeSlotState.Loading
                if (activeRequest?.requestId == requestId) {
                    nativeSlots[slotId] = NativeSlotState.Failed
                }
            },
        )
    }

    internal fun releaseNative(slotId: String) {
        (nativeSlots.remove(slotId) as? NativeSlotState.Loaded)
            ?.handle
            ?.let(provider::destroyNative)
    }

    private fun loadInterstitial(countAsEligibleAttempt: Boolean) {
        if (interstitialState != InterstitialState.Idle) return
        if (countAsEligibleAttempt && eligibleLoadAttempts >= MAX_ELIGIBLE_LOAD_ATTEMPTS) return
        val context = application ?: return
        if (countAsEligibleAttempt) eligibleLoadAttempts += 1
        val requestId = ++interstitialRequestId
        interstitialState = InterstitialState.Loading
        provider.loadInterstitial(
            context = context,
            onLoaded = onLoaded@{
                if (requestId != interstitialRequestId || application == null) return@onLoaded
                interstitialState = InterstitialState.Ready
                if (pendingInterstitial && processInForeground) tryShowInterstitial()
            },
            onFailed = onFailed@{
                if (requestId != interstitialRequestId || application == null) return@onFailed
                interstitialState = InterstitialState.Idle
                scheduleInterstitialRetry()
            },
        )
    }

    private fun tryShowInterstitial() {
        if (!pendingInterstitial || !processInForeground || interstitialState != InterstitialState.Ready) return
        val activity = currentActivity.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return

        val requestId = interstitialRequestId
        interstitialState = InterstitialState.Showing
        val started = provider.showInterstitial(
            activity = activity,
            onDismissed = onDismissed@{
                if (requestId != interstitialRequestId || application == null) return@onDismissed
                interstitialState = InterstitialState.Idle
                pendingInterstitial = false
                eligibleLoadAttempts = 0
                cancelInterstitialRetry()
                policy?.markShown()
            },
            onFailed = onFailed@{
                if (requestId != interstitialRequestId || application == null) return@onFailed
                interstitialState = InterstitialState.Idle
                scheduleInterstitialRetry()
            },
        )
        if (!started && requestId == interstitialRequestId && interstitialState == InterstitialState.Showing) {
            interstitialState = InterstitialState.Idle
            scheduleInterstitialRetry()
        }
    }

    private fun scheduleInterstitialRetry() {
        if (!canRetryInterstitial() || retryScheduled) return
        val completedAttempts = eligibleLoadAttempts.coerceAtLeast(1)
        val retryDelay = BASE_RETRY_DELAY_MS shl (completedAttempts - 1)
        retryScheduled = true
        retryHandler.postDelayed(retryInterstitial, retryDelay)
    }

    private fun canRetryInterstitial(): Boolean =
        application != null &&
            processInForeground &&
            pendingInterstitial &&
            interstitialState == InterstitialState.Idle &&
            eligibleLoadAttempts < MAX_ELIGIBLE_LOAD_ATTEMPTS

    private fun cancelInterstitialRetry() {
        if (!retryScheduled) return
        retryHandler.removeCallbacks(retryInterstitial)
        retryScheduled = false
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
        if (pendingInterstitial) tryShowInterstitial()
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity.get() === activity) currentActivity.clear()
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity.get() === activity) currentActivity.clear()
    }

    private companion object {
        const val PREFERENCES_NAME = "movies_ads_sdk_state"
        const val MAX_ELIGIBLE_LOAD_ATTEMPTS = 3
        const val BASE_RETRY_DELAY_MS = 1_000L
    }
}
