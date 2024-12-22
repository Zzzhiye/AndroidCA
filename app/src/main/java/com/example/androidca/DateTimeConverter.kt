package com.example.androidca

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateTimeConverter {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private val toFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun toString(dateTime: LocalDateTime): String {
        return dateTime.format(toFormatter)
    }

    fun fromString(dateTimeString: String): LocalDateTime {
        return LocalDateTime.parse(dateTimeString, formatter)
    }
}