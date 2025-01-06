package com.example.androidca.api


import com.example.androidca.Ranking
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("api/Rankings/addRanking")
    suspend fun addRanking(@Body rankingRequest: RankingRequest): Response<Unit>

    @GET("api/auth/getUser/{userId}")
    suspend fun getUser(@Path("userId") userId: Int): Response<User>

    @PUT("api/auth/updateUser/{userId}")
    suspend fun updateUser(@Path("userId") userId: Int, @Body updateUserRequest: UserUpdateRequest): Response<User>

    @GET("api/rankings/user/{userId}")
    suspend fun getUserRankings(@Path("userId") userId: Int): Response<List<Ranking>>

    @GET("api/rankings/top/user/{userId}")
    suspend fun getUserTopRanking(@Path("userId") userId: Int): Response<String>
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

data class User(
    val userId: Int,
    val userName: String,
    val email: String,
    val isPaid: Boolean
)

data class RankingRequest(
    val userId: Int,
    val completionTime: String
)

data class UserUpdateRequest(
    val userName: String,
    val email: String
)


