package com.myarulin.movies_test.sdk.internal

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.myarulin.movies_test.sdk.R

internal class AdMobNativeAdHandle(val ad: NativeAd) : NativeAdHandle

internal class AdMobProvider : AdProvider {
    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoading = false

    override fun initialize(context: Context) {
        MobileAds.initialize(context)
    }

    override fun loadNative(
        context: Context,
        onLoaded: (NativeAdHandle) -> Unit,
        onFailed: (String) -> Unit,
    ) {
        AdLoader.Builder(context, TEST_NATIVE_AD_UNIT_ID)
            .forNativeAd { nativeAd -> onLoaded(AdMobNativeAdHandle(nativeAd)) }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    onFailed(error.message)
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    override fun createNativeView(context: Context, handle: NativeAdHandle): View {
        val nativeAd = (handle as AdMobNativeAdHandle).ad
        return NativeAdView(context).apply {
            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16.dp(context), 12.dp(context), 16.dp(context), 12.dp(context))
                setBackgroundColor(Color.rgb(245, 245, 245))
            }

            val sponsored = TextView(context).apply {
                text = context.getString(R.string.sponsored)
                textSize = 11f
                setTextColor(Color.DKGRAY)
            }
            val headline = TextView(context).apply {
                textSize = 18f
                setTextColor(Color.BLACK)
            }
            val media = MediaView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    180.dp(context),
                )
            }
            val body = TextView(context).apply {
                textSize = 14f
                setTextColor(Color.DKGRAY)
            }
            val icon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(48.dp(context), 48.dp(context))
            }
            val action = Button(context)

            content.addView(sponsored)
            content.addView(headline)
            content.addView(media)
            content.addView(icon)
            content.addView(body)
            content.addView(action)
            addView(
                content,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )

            headlineView = headline
            mediaView = media
            bodyView = body
            iconView = icon
            callToActionView = action

            headline.text = nativeAd.headline
            body.text = nativeAd.body
            body.visibility = if (nativeAd.body.isNullOrBlank()) View.GONE else View.VISIBLE
            icon.setImageDrawable(nativeAd.icon?.drawable)
            icon.visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
            action.text = nativeAd.callToAction
            action.visibility = if (nativeAd.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
            setNativeAd(nativeAd)
        }
    }

    override fun destroyNative(handle: NativeAdHandle) {
        (handle as? AdMobNativeAdHandle)?.ad?.destroy()
    }

    override fun loadInterstitial(
        context: Context,
        onLoaded: () -> Unit,
        onFailed: () -> Unit,
    ) {
        if (interstitialAd != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitialAd = ad
                    onLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    interstitialAd = null
                    onFailed()
                }
            },
        )
    }

    override fun showInterstitial(
        activity: Activity,
        onDismissed: () -> Unit,
        onFailed: () -> Unit,
    ): Boolean {
        val ad = interstitialAd ?: return false
        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = onDismissed()

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                onFailed()
            }
        }
        ad.show(activity)
        return true
    }

    override fun release() {
        interstitialAd = null
    }

    private fun Int.dp(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
        const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    }
}
