package com.example.androidca

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL


class LeaderBoardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("into LeaderBoard")
        setContentView(R.layout.activity_leaderboard)

        val extras = intent.extras
        val formatUserScore: String = if (extras != null) {
            var formattedValue: String? = null
            for (key in extras.keySet()) {
                val value = extras[key]
                formattedValue = when (value) {
                    is String -> "Your best timing: $value"
                    is Long -> "Your completion timing: "+ formatTimeSpan(value)
                    else -> value.toString()
                }
            }
            formattedValue ?: "No score available"
        } else {
            "No score available"
        }

        val closeButton = findViewById<ImageButton>(R.id.closeBtn)
        closeButton.setBackgroundResource(R.drawable.close)
        closeButton.setOnClickListener {

            val intent = Intent(this, FetchActivity::class.java)
            startActivity(intent)
            finish()
        }

        CoroutineScope(Dispatchers.Main).launch {
            val rankingList: List<Ranking>? = withContext(Dispatchers.IO) {
                fetchData()
            }

            if (rankingList == null) {
                println("List is null")
            } else {
                val listView = findViewById<ListView>(R.id.listView)
                listView?.adapter = MyCustomAdapter(this@LeaderBoardActivity, rankingList!!)
                println("List is not null and has ${rankingList!!.size} items")

                val curr = this@LeaderBoardActivity!!.findViewById<TextView>(R.id.currentScore)
                curr?.text = formatUserScore
                curr?.setBackgroundResource(R.drawable.footer)

                val mediaPlayer = MediaPlayer.create(this@LeaderBoardActivity, R.raw.applause)
                mediaPlayer.start()
            }
        }
    }


    fun fetchData() : List<Ranking>? {
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
                val rankingList : List<Ranking> = Json.decodeFromString(response)
                for (ranking in rankingList) {
                    ranking.dateTime = ranking.dateTime.replace(Regex("\\.\\d+$"), "")
                }
                return rankingList
            } else {
                println("Error: ${conn.responseCode}")
                return null
            }
        } catch (e: Exception) {
            println("HTTP Exception occurred: ${e.message}")
            e.printStackTrace()
            return null
        } finally {
            conn?.disconnect()
        }
    }

    private fun formatTimeSpan(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        if(seconds>3600){
            return String.format("%dh %02dmin :%02ds", hours, minutes, secs)
        }else if(seconds>60){
            return String.format("%dmin %02ds", minutes, secs)
        }else{
            return String.format("%ds", secs)
        }
    }
}
