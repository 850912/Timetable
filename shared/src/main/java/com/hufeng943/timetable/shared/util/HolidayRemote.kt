package com.hufeng943.timetable.shared.util

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.URL

suspend fun fetchRemoteHolidayGreeting(date: LocalDate): HolidayGreeting? = runCatching {
    val connection = (URL("https://date.nager.at/api/v3/PublicHolidays/${date.year}/CN").openConnection() as HttpURLConnection).apply {
        connectTimeout = 2500
        readTimeout = 2500
        requestMethod = "GET"
    }
    connection.inputStream.bufferedReader().use { reader ->
        val rows = Json.parseToJsonElement(reader.readText()).jsonArray
        rows.firstOrNull { it.jsonObject["date"]?.toString()?.trim('"') == date.toString() }?.jsonObject?.let { row ->
            val name = row["localName"]?.toString()?.trim('"') ?: row["name"]?.toString()?.trim('"') ?: return@let null
            HolidayGreeting(name, "${name}快乐，愿今天有温暖和好心情！")
        }
    }
}.getOrNull()
