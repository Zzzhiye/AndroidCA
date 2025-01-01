package com.example.androidca.repository

import com.example.androidca.api.ApiClient
import com.example.androidca.api.User
import com.example.androidca.api.UserUpdateRequest
import retrofit2.Response

class UserRepository {
    private val apiService = ApiClient.apiService

    // 获取用户信息
    suspend fun getUser(userId: Int): Response<User> {
        return apiService.getUser(userId)
    }

    // 更新用户信息
    suspend fun updateUser(userId: Int, userName: String, email: String): Response<User> {
        val request = UserUpdateRequest(userName, email)
        return apiService.updateUser(userId, request)
    }
}
