package com.example.androidca

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.androidca.databinding.ActivityVerifyBinding

class VerifyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyBinding

    private lateinit var digits: List<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        digits = listOf(digits[0], digits[1], digits[3], digits[4])

        setupDigitInputs()

        binding.verifyButton.setOnClickListener {
            val code = digits.joinToString("") { it.text.toString() }
            if (verifyCode(code)) {
                startActivity(Intent(this, VerifyActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Wrong Verification", Toast.LENGTH_SHORT).show()
            }
        }

        binding.resendCode.setOnClickListener {
            // resend
            sendVerificationCode()
        }
    }

    private fun setupDigitInputs() {
        for (i in digits.indices) {
            digits[i].addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < digits.size - 1) {
                        digits[i + 1].requestFocus()
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    private fun verifyCode(code: String): Boolean {
        val sharedPrefs = getSharedPreferences(LoginActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = sharedPrefs.getString(LoginActivity.VERIFICATION_CODE_KEY, "")
        return code == savedCode
    }

    private fun sendVerificationCode() {
        // 重新发送验证码的逻辑
    }
}