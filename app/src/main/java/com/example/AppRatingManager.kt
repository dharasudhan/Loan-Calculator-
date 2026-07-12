package com.example

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

object AppRatingManager {
    private const val PREFS_NAME = "app_rating_prefs"
    private const val KEY_APP_OPENS = "app_opens"
    private const val KEY_CALCULATIONS = "calculations"
    private const val KEY_RATING_REQUESTED = "rating_requested"
    
    // Conditions for requesting rating
    private const val MIN_APP_OPENS = 3
    private const val MIN_CALCULATIONS = 5

    fun trackAppOpen(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val opens = prefs.getInt(KEY_APP_OPENS, 0)
        prefs.edit().putInt(KEY_APP_OPENS, opens + 1).apply()
    }

    fun trackCalculation(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val calcs = prefs.getInt(KEY_CALCULATIONS, 0)
        prefs.edit().putInt(KEY_CALCULATIONS, calcs + 1).apply()
    }

    fun maybeRequestRating(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val opens = prefs.getInt(KEY_APP_OPENS, 0)
        val calcs = prefs.getInt(KEY_CALCULATIONS, 0)
        val requested = prefs.getBoolean(KEY_RATING_REQUESTED, false)

        if (!requested && opens >= MIN_APP_OPENS && calcs >= MIN_CALCULATIONS) {
            requestRating(activity, prefs)
        }
    }

    private fun requestRating(activity: Activity, prefs: SharedPreferences) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished.
                    prefs.edit().putBoolean(KEY_RATING_REQUESTED, true).apply()
                }
            } else {
                Log.e("AppRatingManager", "In-app review request failed", task.exception)
            }
        }
    }
}
