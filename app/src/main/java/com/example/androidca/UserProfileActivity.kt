package com.example.androidca

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.androidca.R
import com.example.androidca.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserProfileActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val userIdText = findViewById<TextView>(R.id.userIdText)
        val userNameText = findViewById<TextView>(R.id.userNameText)
        val emailText = findViewById<TextView>(R.id.emailText)
        val editButton = findViewById<Button>(R.id.editButton)
        val backButton = findViewById<Button>(R.id.backButton)
        val userId = 1 // 示例值，实际情况下应根据登录信息动态获取

        // 获取用户信息
        CoroutineScope(Dispatchers.IO).launch {
            val response = ApiClient.apiService.getUser(userId)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.let {
                        userIdText.text = " ${it.userId}"
                        userNameText.text = " ${it.userName}"
                        emailText.text = " ${it.email}"
                    }
                } else {
                    Toast.makeText(this@UserProfileActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
            }
        }


        backButton.setOnClickListener {
            val intent = Intent(this, FetchActivity::class.java)
            startActivity(intent)
            finish()
        }

        // 跳转到编辑界面
        editButton.setOnClickListener {
            val cleanUserName = userNameText.text.toString().trim().replace("\\s+", " ").replace("[^a-zA-Z0-9 ]", "")
            val cleanEmail = emailText.text.toString().trim()
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("UserId", userId)
            intent.putExtra("UserName", cleanUserName)
            intent.putExtra("Email", cleanEmail)
            startActivity(intent)
        }
    }
}