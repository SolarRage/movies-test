package com.myarulin.movies_test.sdk

import com.myarulin.movies_test.sdk.internal.AdMobProvider
import com.myarulin.movies_test.sdk.internal.DefaultAdsManager
import org.koin.core.module.Module
import org.koin.dsl.module

fun adsSdkModule(
    config: AdsConfig = AdsConfig(),
    providerFactory: (AdsConfig) -> AdProvider = ::AdMobProvider,
): Module = module {
    single<AdProvider> { providerFactory(config) }
    single<AdsManager> { DefaultAdsManager(provider = get()) }
}

val sdkModule = adsSdkModule()
