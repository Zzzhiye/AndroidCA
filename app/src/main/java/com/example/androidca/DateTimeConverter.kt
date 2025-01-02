package com.example.androidca

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateTimeConverter {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private val toDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val toSecFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun toDayString(dateTime: LocalDateTime): String {
        return dateTime.format(toDayFormatter)
    }

    fun toSecString(dateTime: LocalDateTime): String {
        return dateTime.format(toSecFormatter)
    }

    fun fromString(dateTimeString: String): LocalDateTime {
        return LocalDateTime.parse(dateTimeString, formatter)
    }
}