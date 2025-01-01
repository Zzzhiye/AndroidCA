package com.example.androidca

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.androidca.R
import com.example.androidca.api.ApiClient
import com.example.androidca.api.UserUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val userNameEditText = findViewById<EditText>(R.id.userNameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        // 获取传递过来的用户信息
        val userId = intent.getIntExtra("UserId", -1)
        val currentUserName = intent.getStringExtra("UserName")
        val currentEmail = intent.getStringExtra("Email")

        // 显示当前用户信息
        userNameEditText.setText(currentUserName)
        emailEditText.setText(currentEmail)

        // 保存更新后的用户信息
        saveButton.setOnClickListener {
            val newUserName = userNameEditText.text.toString()
            val newEmail = emailEditText.text.toString()

            CoroutineScope(Dispatchers.IO).launch {
                val response = ApiClient.apiService.updateUser(
                    userId,
                    UserUpdateRequest(newUserName, newEmail)
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@EditProfileActivity,
                            "Profile updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@EditProfileActivity,
                            "Failed to update profile",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}