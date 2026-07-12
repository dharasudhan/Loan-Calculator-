package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

object InterstitialAdHelper {
    private const val TAG = "InterstitialAdHelper"
    
    

    private var mInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    fun isAdReady(): Boolean {
        return mInterstitialAd != null
    }

    fun loadAd(context: Context) {
        if (mInterstitialAd != null || isAdLoading) {
            return
        }
        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            context.getString(R.string.admob_interstitial_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Ad failed to load: ${adError.message}")
                    mInterstitialAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                    isAdLoading = false
                }
            }
        )
    }

    fun showAdIfReady(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = mInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    mInterstitialAd = null
                    // Pre-load the next ad
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.d(TAG, "Ad failed to show: ${adError.message}")
                    mInterstitialAd = null
                    // Pre-load the next ad
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "Ad was not ready yet.")
            // Try to trigger a load in the background for next time
            loadAd(activity)
            onAdDismissed()
        }
    }
}
