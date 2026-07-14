package com.example

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class ConsentManager(private val context: Context) {
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    fun gatherConsent(
        activity: Activity,
        onConsentGathered: () -> Unit
    ) {
        // Set tag for under age of consent. false means users are not under age
        // of consent.
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // The consent information state was updated.
                // You are now ready to check if a form is available.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    activity
                ) { formError ->
                    if (formError != null) {
                        Log.w("ConsentManager", formError.message)
                    }

                    if (canRequestAds) {
                        onConsentGathered()
                    }
                }
            },
            { requestConsentError ->
                Log.w("ConsentManager", requestConsentError.message)
                if (canRequestAds) {
                    onConsentGathered()
                }
            }
        )
        
        // Check if you can initialize the Google Mobile Ads SDK in parallel
        // while checking for new consent information. Consent obtained in
        // the previous session can be used to request ads.
        if (canRequestAds) {
            onConsentGathered()
        }
    }

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()
}
