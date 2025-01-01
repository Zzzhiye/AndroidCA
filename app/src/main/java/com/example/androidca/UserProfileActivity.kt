package com.example.androidca

import RankingAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidca.LoginActivity.Companion.SHARED_PREFS_NAME
import com.example.androidca.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log // 添加日志功能
import androidx.recyclerview.widget.RecyclerView

class UserProfileActivity : AppCompatActivity() {
    private lateinit var rankingsRecyclerView: RecyclerView // 定义 RecyclerView 变量

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val userIdText = findViewById<TextView>(R.id.userIdText)
        val userNameText = findViewById<TextView>(R.id.userNameText)
        val emailText = findViewById<TextView>(R.id.emailText)
        val editButton = findViewById<Button>(R.id.editButton)
        val backButton = findViewById<Button>(R.id.backButton)
        rankingsRecyclerView = findViewById(R.id.rankingsRecyclerView) // 初始化 RecyclerView

        val sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        backButton.setOnClickListener {
            onBackPressed()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userResponse = ApiClient.apiService.getUser(userId)
                withContext(Dispatchers.Main) {
                    if (userResponse.isSuccessful) {
                        val user = userResponse.body()
                        user?.let {
                            userIdText.text = "${it.userId}"
                            userNameText.text = "${it.userName}"
                            emailText.text = "${it.email}"

                            // 加载用户历史记录
                            loadRankings(it.userId)
                        }
                    } else {
                        Log.e("UserProfileActivity", "Failed to load user data: ${userResponse.errorBody()?.string()}")
                        Toast.makeText(this@UserProfileActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("UserProfileActivity", "Error loading user data", e)
            }
        }

        editButton.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("UserId", userId)
            intent.putExtra("UserName", userNameText.text.toString())
            intent.putExtra("Email", emailText.text.toString())
            startActivity(intent)
        }
    }

    private fun loadRankings(userId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rankingsResponse = ApiClient.apiService.getUserRankings(userId)
                withContext(Dispatchers.Main) {
                    if (rankingsResponse.isSuccessful) {
                        val rankings = rankingsResponse.body()
                        if (rankings.isNullOrEmpty()) {
                            Toast.makeText(this@UserProfileActivity, "No rankings available", Toast.LENGTH_SHORT).show()
                        } else {
                            val adapter = RankingAdapter(rankings)
                            rankingsRecyclerView.layoutManager = LinearLayoutManager(this@UserProfileActivity)
                            rankingsRecyclerView.adapter = adapter
                        }
                    } else {
                        Log.e("UserProfileActivity", "Failed to load rankings: ${rankingsResponse.errorBody()?.string()}")
                        Toast.makeText(this@UserProfileActivity, "Failed to load rankings", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("UserProfileActivity", "Error loading rankings", e)
            }
        }
    }
}

