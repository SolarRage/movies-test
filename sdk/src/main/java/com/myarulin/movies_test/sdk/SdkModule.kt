package com.myarulin.movies_test.sdk

import com.myarulin.movies_test.sdk.internal.AdMobProvider
import com.myarulin.movies_test.sdk.internal.AdProvider
import com.myarulin.movies_test.sdk.internal.DefaultAdsManager
import org.koin.dsl.module

val sdkModule = module {
    single<AdProvider> { AdMobProvider() }
    single<AdsManager> { DefaultAdsManager(provider = get()) }
}
