package com.example.androidca.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
    
    @POST("api/auth/logout")
    suspend fun logout(): Response<LogoutResponse>

    @POST("api/Rankings/addRanking")
    suspend fun addRanking(@Body rankingRequest: RankingRequest): Response<Unit>

}

data class LoginRequest(
    val userName: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val user: User,
    val IsPaid: Boolean,
    val userId: Int
)

data class LogoutResponse(
    val message: String
)

data class User(
    val userId: Int,
    val userName: String,
    val isPaid: Boolean
)

data class RankingRequest(
    val userId: Int,
    val completionTime: String
)