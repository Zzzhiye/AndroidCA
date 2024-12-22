package com.example.androidca

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    private lateinit var adView: AdView
    private val userId = 1234
    private val adHandler = Handler()
    private val adRefreshInterval: Long = 30000 // 30 seconds ad interval

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var intent = Intent(this,PlayActivity::class.java)
        startActivity(intent)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //Ads display logic
        MobileAds.initialize(this) {}
        adView = findViewById(R.id.adView)
        fetchPaidStatus(userId)
        startAdRefreshTask()

        }

    private fun fetchPaidStatus(userId: Int) {
        val call = ApiHelper.api.getPaidStatus(userId)
        call.enqueue(object : Callback<PaidStatusResponse> {
            override fun onResponse(
                call: Call<PaidStatusResponse>,
                response: Response<PaidStatusResponse>
            ) {
                if (response.isSuccessful) {
                    val isPaid = response.body()?.isPaid ?: false
                    if (!isPaid) {
                        AdUtils.loadAd(adView) // Load ad if user is not paid
                    } else {
                        Log.d("MainActivity", "User is paid. Ads are disabled.")
                    }
                } else {
                    Log.e("MainActivity", "Error fetching paid status: ${response.errorBody()}")
                }
            }

            override fun onFailure(call: Call<PaidStatusResponse>, t: Throwable) {
                Log.e("MainActivity", "API call failed: ${t.message}")
            }
        })
    }
    private fun startAdRefreshTask() {
        adHandler.postDelayed(object:Runnable {
            override fun run() {
                AdUtils.loadAd(adView)
                adHandler.postDelayed(this, adRefreshInterval)
            }
        }, adRefreshInterval)
    }

    override fun onDestroy() {
        super.onDestroy()
        adHandler.removeCallbacksAndMessages(null)
    }


    }
