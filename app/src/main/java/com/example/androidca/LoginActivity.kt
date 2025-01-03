package com.example.androidca

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.androidca.api.ApiClient
import com.example.androidca.api.LoginRequest
import com.example.androidca.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    companion object {
        const val VERIFICATION_CODE_KEY = "verification_code"
        const val SHARED_PREFS_NAME = "MemoryGamePrefs"
        const val IS_PAID_KEY = "is_paid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.login(LoginRequest(username, password))
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()?.user
                        val isPaid = user?.isPaid?: false
                        savePaidStatus(isPaid)
                        val userId = user?.userId?: 0
                        saveUserIdToSharedPrefs(userId)
                        sessionManager.createLoginSession(userId)
                        startActivity(Intent(this@LoginActivity, FetchActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //save paid status from response
    private fun savePaidStatus(isPaid: Boolean) {
        val sharedPrefs = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putBoolean(IS_PAID_KEY, isPaid)
            apply()
        }
    }

    private fun saveUserIdToSharedPrefs(userId: Int) {
        val sharedPrefs = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putInt("userId", userId) // 保存 userId
            apply()
        }
    }
}