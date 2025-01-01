package com.example.androidca

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.androidca.LoginActivity.Companion.SHARED_PREFS_NAME
import com.example.androidca.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val userIdText = findViewById<TextView>(R.id.userIdText)
        val userNameText = findViewById<TextView>(R.id.userNameText)
        val emailText = findViewById<TextView>(R.id.emailText)
        val editButton = findViewById<Button>(R.id.editButton)

        // 从 SharedPreferences 获取 UserId
        val sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)

        // 验证是否获取到有效的 UserId
        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 使用 UserId 获取用户信息
        CoroutineScope(Dispatchers.IO).launch {
            val response = ApiClient.apiService.getUser(userId)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.let {
                        userIdText.text = "${it.userId}"
                        userNameText.text = "${it.userName}"
                        emailText.text = "${it.email}"
                    }
                } else {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "Failed to load user data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // 跳转到编辑界面
        editButton.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("UserId", userId)
            intent.putExtra("UserName", userNameText.text.toString())
            intent.putExtra("Email", emailText.text.toString())
            startActivity(intent)
        }
    }
}
