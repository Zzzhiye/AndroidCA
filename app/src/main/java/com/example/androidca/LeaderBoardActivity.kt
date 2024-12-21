package com.example.androidca

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

class LeaderBoardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("into LeaderBoard")
        setContentView(R.layout.activity_leaderboard)

        Thread {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("http://10.0.2.2:5266/api/rankings")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                println("before connect")
                conn.connect()
                println("after connect ")
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    println("Response: $response")
                } else {
                    println("Error: ${conn.responseCode}")
                }
            } catch (e: Exception) {
                println("HTTP Exception occurred: ${e.message}")
                e.printStackTrace()
            } finally {
                conn?.disconnect()
            }
        }.start()
    }

    data class Ranking(
        val activityId: Int,
        val userName: String,
        val completeTime: Int,
        val dateTime: LocalDateTime
    )
}
