package com.hufeng943.timetable.logging

import android.util.Log

/** Single logging entry point for the Wear module. */
object AppLogger {
    fun debug(tag: String, message: String) = Log.d(tag, message)
    fun info(tag: String, message: String) = Log.i(tag, message)
    fun warn(tag: String, message: String, throwable: Throwable? = null) =
        if (throwable == null) Log.w(tag, message) else Log.w(tag, message, throwable)
    fun error(tag: String, message: String, throwable: Throwable? = null) =
        if (throwable == null) Log.e(tag, message) else Log.e(tag, message, throwable)
}
