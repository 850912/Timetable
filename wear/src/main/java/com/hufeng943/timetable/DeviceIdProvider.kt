package com.hufeng943.timetable

import android.content.Context
import android.provider.Settings

object DeviceIdProvider {
    fun get(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() }
            ?: "ANDROID-${context.packageName}"
    }
}
