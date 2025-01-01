package com.example.androidca

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class AdManager(private val context: Context, private val adContainer: FrameLayout) {
    private lateinit var adView: AdView
    private val refreshInterval = 30000L // 30 seconds
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var isPaidUser = false

    // Initialize and manage ads
    fun initializeAds() {
        // Initialize the Mobile Ads SDK
        MobileAds.initialize(context) {}

        // Check user paid status and decide whether to show ads
       isPaidUser = checkUserPaidStatus()
        if (isPaidUser) {
            adContainer.visibility = View.GONE // Hide ads for paid users
            return
        }

        // Create and set up the AdView
        adView = AdView(context)
        adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER)
        adView.adUnitId = "ca-app-pub-6677345918902926/4778437587"

        adContainer.addView(adView)
        startAdRefreshing()
    }

    private fun checkUserPaidStatus(): Boolean {
        val sharedPrefs = context.getSharedPreferences(LoginActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean(LoginActivity.IS_PAID_KEY, false)
    }

    private fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun startAdRefreshing() {
        loadAd()
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isPaidUser && adView!=null) {
                    loadAd()
                    handler.postDelayed(this, refreshInterval)
                }
            }
        }, refreshInterval)
    }

    // Stop ad loading and refreshing
    fun stopAds() {
        handler.removeCallbacksAndMessages(null)
        adView.destroy()
        adContainer.removeAllViews()
    }
}
