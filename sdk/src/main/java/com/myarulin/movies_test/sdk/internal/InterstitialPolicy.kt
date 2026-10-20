package com.myarulin.movies_test.sdk.internal

internal interface InterstitialSessionStore {
    var foregroundCount: Int
    var interstitialShown: Boolean
}

internal class InterstitialPolicy(
    private val store: InterstitialSessionStore,
) {
    val canShowAgain: Boolean
        get() = !store.interstitialShown

    fun onForeground(): Boolean {
        if (store.interstitialShown) return false
        store.foregroundCount += 1
        return store.foregroundCount == SECOND_FOREGROUND
    }

    fun markShown() {
        store.interstitialShown = true
    }

    private companion object {
        const val SECOND_FOREGROUND = 2
    }
}
