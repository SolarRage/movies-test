package com.myarulin.movies_test.sdk.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InterstitialPolicyTest {
    @Test
    fun `interstitial becomes eligible only on second foreground`() {
        val store = InMemorySessionStore()
        val policy = InterstitialPolicy(store)

        assertThat(policy.onForeground()).isFalse()
        assertThat(policy.onForeground()).isTrue()
        assertThat(policy.onForeground()).isFalse()
    }

    @Test
    fun `shown interstitial is never eligible again`() {
        val store = InMemorySessionStore(foregroundCount = 1)
        val policy = InterstitialPolicy(store)

        assertThat(policy.onForeground()).isTrue()
        policy.markShown()

        assertThat(policy.onForeground()).isFalse()
        assertThat(store.interstitialShown).isTrue()
        assertThat(policy.canShowAgain).isFalse()
    }
}

private data class InMemorySessionStore(
    override var foregroundCount: Int = 0,
    override var interstitialShown: Boolean = false,
) : InterstitialSessionStore
