package com.myarulin.movies_test.sdk.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.myarulin.movies_test.sdk.NativeAdsLoadState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultAdsManagerTest {
    private lateinit var provider: FakeAdProvider
    private lateinit var manager: DefaultAdsManager
    private lateinit var lifecycleOwner: TestLifecycleOwner

    @Before
    fun setUp() = onMainThread {
        provider = FakeAdProvider()
        lifecycleOwner = TestLifecycleOwner()
        manager = DefaultAdsManager(
            provider = provider,
            sessionStoreFactory = { TestSessionStore() },
            processLifecycle = lifecycleOwner.lifecycle,
        )
        manager.initialize(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() = onMainThread {
        manager.release()
    }

    @Test
    fun failedInitialLoadIsRetriedInEligibleForeground() = onMainThread {
        provider.interstitialLoads.single().onFailed()

        manager.onStart(lifecycleOwner)
        manager.onStop(lifecycleOwner)
        manager.onStart(lifecycleOwner)

        assertThat(provider.interstitialLoads).hasSize(2)
    }

    @Test
    fun nativeAdLoadedAfterSlotReleaseIsDestroyed() = onMainThread {
        manager.loadNative("search:1")
        val pendingLoad = provider.nativeLoads.single()
        val handle = FakeNativeAdHandle()

        manager.releaseNative("search:1")
        pendingLoad.onLoaded(handle)

        assertThat(provider.destroyedNativeAds).containsExactly(handle)
    }

    @Test
    fun nativeAdsRemainLoadingUntilEverySlotFinishes() = onMainThread {
        val slotIds = setOf("search:1", "search:3")
        manager.loadNative("search:1")
        manager.loadNative("search:3")

        assertThat(manager.nativeAdsLoadState(slotIds)).isEqualTo(NativeAdsLoadState.Loading)

        provider.nativeLoads[0].onLoaded(FakeNativeAdHandle())
        assertThat(manager.nativeAdsLoadState(slotIds)).isEqualTo(NativeAdsLoadState.Loading)

        provider.nativeLoads[1].onLoaded(FakeNativeAdHandle())
        assertThat(manager.nativeAdsLoadState(slotIds)).isEqualTo(NativeAdsLoadState.Ready)
    }

    @Test
    fun nativeAdFailureCompletesPreparationWithoutBlockingContent() = onMainThread {
        val slotIds = setOf("search:1", "search:3")
        manager.loadNative("search:1")
        manager.loadNative("search:3")

        provider.nativeLoads[0].onFailed("offline")
        provider.nativeLoads[1].onLoaded(FakeNativeAdHandle())

        assertThat(manager.nativeAdsLoadState(slotIds)).isEqualTo(NativeAdsLoadState.Failed)
    }

    @Test
    fun interstitialLoadedAfterActivityPauseIsNotShown() = onMainThread {
        manager.onStart(lifecycleOwner)
        manager.onStop(lifecycleOwner)
        manager.onStart(lifecycleOwner)
        val activity = Activity()
        manager.onActivityResumed(activity)

        manager.onActivityPaused(activity)
        provider.interstitialLoads.single().onLoaded()

        assertThat(provider.showCalls).isEqualTo(0)
    }

    private fun onMainThread(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }
}

private class TestLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = registry
}

private data class TestSessionStore(
    override var foregroundCount: Int = 0,
    override var interstitialShown: Boolean = false,
) : InterstitialSessionStore

private class FakeNativeAdHandle : NativeAdHandle

private data class NativeLoad(
    val onLoaded: (NativeAdHandle) -> Unit,
    val onFailed: (String) -> Unit,
)

private data class InterstitialLoad(
    val onLoaded: () -> Unit,
    val onFailed: () -> Unit,
)

private class FakeAdProvider : AdProvider {
    val nativeLoads = mutableListOf<NativeLoad>()
    val interstitialLoads = mutableListOf<InterstitialLoad>()
    val destroyedNativeAds = mutableListOf<NativeAdHandle>()
    var showCalls = 0

    override fun initialize(context: Context) = Unit

    override fun loadNative(
        context: Context,
        onLoaded: (NativeAdHandle) -> Unit,
        onFailed: (String) -> Unit,
    ) {
        nativeLoads += NativeLoad(onLoaded, onFailed)
    }

    override fun createNativeView(context: Context, handle: NativeAdHandle): View = View(context)

    override fun destroyNative(handle: NativeAdHandle) {
        destroyedNativeAds += handle
    }

    override fun loadInterstitial(
        context: Context,
        onLoaded: () -> Unit,
        onFailed: () -> Unit,
    ) {
        interstitialLoads += InterstitialLoad(onLoaded, onFailed)
    }

    override fun showInterstitial(
        activity: Activity,
        onDismissed: () -> Unit,
        onFailed: () -> Unit,
    ): Boolean {
        showCalls += 1
        return true
    }

    override fun release() = Unit
}
