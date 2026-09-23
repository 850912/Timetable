package com.hufeng943.timetable.sync

import android.content.Context
import android.content.Intent
import com.hufeng943.timetable.shared.importexport.WearProfileRequest

object WearProfileIntentRouter {
    fun open(context: Context, request: WearProfileRequest) {
        val intent = Intent(context, WearProfileOpenReceiverService::class.java)
            .putExtra(WearProfileOpenReceiverService.EXTRA_TARGET, request.target)
            .putExtra(WearProfileOpenReceiverService.EXTRA_APP_URI, request.appUri)
            .putExtra(WearProfileOpenReceiverService.EXTRA_FALLBACK_URL, request.fallbackUrl)
        context.startService(intent)
    }

    fun open(context: Context, target: String) {
        val request = when (target) {
            "douyin" -> WearProfileRequest("douyin", "snssdk1128://user/homepage", "https://www.douyin.com")
            "coolapk" -> WearProfileRequest("coolapk", "coolmarket://u/22532694", "https://www.coolapk.com/u/22532694")
            else -> WearProfileRequest(target, null, "https://github.com")
        }
        open(context, request)
    }
}
