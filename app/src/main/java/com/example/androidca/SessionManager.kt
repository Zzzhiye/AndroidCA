package com.example.androidca

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor = prefs.edit()

    companion object {
        private const val PREF_NAME = "UserSession"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_LOGIN_TIME = "loginTime"
        private const val KEY_USER_ID = "userId"
        private const val SESSION_DURATION = 1 * 60 * 1000 //ms
    }

    fun createLoginSession(userId: Int) {
        editor.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            putInt(KEY_USER_ID, userId)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return false
        }

        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0)
        val currentTime = System.currentTimeMillis()
        
        return if (currentTime - loginTime < SESSION_DURATION) {
            true
        } else {
            logoutUser()
            false
        }
    }

    fun logoutUser() {
        editor.apply {
            clear()
            apply()
        }
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun getSessionExpiryTime(): Long {
        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0)
        return loginTime + SESSION_DURATION
    }
}
