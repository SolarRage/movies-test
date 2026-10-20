package com.myarulin.movies_test.sdk.internal

import com.google.common.truth.Truth.assertThat
import com.myarulin.movies_test.sdk.AdsManager
import org.junit.Test

class AdsManagerAbiTest {
    // Guards against Compose adding different synthetic parameters to an interface method and
    // its implementation. A mismatch previously compiled but crashed at runtime with AbstractMethodError.
    @Test
    fun `composable implementations match interface ABI`() {
        listOf("prepareNativeAds", "NativeAd").forEach { methodName ->
            val interfaceMethod = AdsManager::class.java.methods.single { it.name == methodName }
            val implementationMethod = DefaultAdsManager::class.java.methods.single { it.name == methodName }

            assertThat(implementationMethod.parameterTypes.toList())
                .containsExactlyElementsIn(interfaceMethod.parameterTypes.toList())
                .inOrder()
            assertThat(implementationMethod.returnType).isEqualTo(interfaceMethod.returnType)
        }
    }
}
