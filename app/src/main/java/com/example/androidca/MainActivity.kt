package com.example.androidca

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val startBtn = findViewById<Button>(R.id.startButton)
        startBtn.setOnClickListener {
            if (isUserLoggedIn()) {
                startActivity(Intent(this, FetchActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
    }

    fun setUserLoggedIn() {
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_logged_in", true)
            .apply()
    }

    private fun isUserLoggedIn(): Boolean {
        return getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_logged_in", false)
    }
}