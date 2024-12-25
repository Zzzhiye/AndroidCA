package com.example.androidca

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.androidca.LoginActivity.Companion.IS_PAID_KEY
import com.example.androidca.LoginActivity.Companion.SHARED_PREFS_NAME
import com.example.androidca.LoginActivity.Companion.VERIFICATION_CODE_KEY
import com.example.androidca.api.ApiClient
import com.example.androidca.api.LoginRequest
import com.example.androidca.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import retrofit2.HttpException
import kotlin.random.Random

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    companion object {
        const val VERIFICATION_CODE_KEY = "verification_code"
        const val SHARED_PREFS_NAME = "MemoryGamePrefs"
        const val IS_PAID_KEY = "is_paid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.login(LoginRequest(username, password))
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        val isPaid = loginResponse?.user?.isPaid?: false
                        savePaidStatus(isPaid)
                        startActivity(Intent(this@LoginActivity, PlayActivity::class.java))
                        finish()
                        }  else {
                        Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validateCredentials(username: String, password: String): Boolean {
        return username.isNotEmpty() && password.isNotEmpty()
    }

    private fun generateVerificationCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    private fun saveVerificationCode(code: String) {
        val sharedPrefs = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString(VERIFICATION_CODE_KEY, code)
            apply()
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
}