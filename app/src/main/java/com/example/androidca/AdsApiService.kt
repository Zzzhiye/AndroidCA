package com.example.androidca
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class PaidStatusResponse(val isPaid: Boolean)

interface AdsApiService {
    @GET("api/user/{UserId}/paid-status")
    fun getPaidStatus(@Path("UserId") UserId: Int): Call<PaidStatusResponse>
}

object ApiHelper {
    private const val BASE_URL = "http://localhost:5000/" //backendurl
    val api: AdsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdsApiService::class.java)
    }
}

object AdUtils{
    fun loadAd(adView: AdView) {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }
}

