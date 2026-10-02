package com.hufeng943.timetable.shared.util

import kotlinx.datetime.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.HttpURLConnection
import java.net.URL

suspend fun fetchRemoteHolidayGreeting(date: LocalDate): HolidayGreeting? = withContext(Dispatchers.IO) {
    val connection = (URL("https://date.nager.at/api/v3/PublicHolidays/${date.year}/CN").openConnection() as HttpURLConnection).apply {
        connectTimeout = 2500
        readTimeout = 2500
        requestMethod = "GET"
    }
    try {
        connection.inputStream.bufferedReader().use { reader ->
            val rows = Json.parseToJsonElement(reader.readText()).jsonArray
            rows.firstOrNull { it.jsonObject["date"]?.jsonPrimitive?.contentOrNull == date.toString() }?.jsonObject?.let { row ->
                val name = row["localName"]?.jsonPrimitive?.contentOrNull ?: row["name"]?.jsonPrimitive?.contentOrNull ?: return@let null
                HolidayGreeting(name, "${name}快乐，愿今天有温暖和好心情！")
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}
