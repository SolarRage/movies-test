package com.myarulin.movies_test.sdk.internal

import android.content.SharedPreferences
import androidx.core.content.edit

internal class SharedPreferencesSessionStore(
    private val preferences: SharedPreferences,
) : InterstitialSessionStore {
    override var foregroundCount: Int
        get() = preferences.getInt(KEY_FOREGROUND_COUNT, 0)
        set(value) = preferences.edit { putInt(KEY_FOREGROUND_COUNT, value) }

    override var interstitialShown: Boolean
        get() = preferences.getBoolean(KEY_INTERSTITIAL_SHOWN, false)
        set(value) = preferences.edit { putBoolean(KEY_INTERSTITIAL_SHOWN, value) }

    private companion object {
        const val KEY_FOREGROUND_COUNT = "foreground_count"
        const val KEY_INTERSTITIAL_SHOWN = "interstitial_shown"
    }
}
