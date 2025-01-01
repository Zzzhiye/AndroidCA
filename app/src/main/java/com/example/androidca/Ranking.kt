package com.example.androidca
import kotlinx.serialization.Serializable

@Serializable
data class Ranking(
    val activityId: Int,
    val userName: String,
    val completionTime: String,
    var dateTime: String
)
