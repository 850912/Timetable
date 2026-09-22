
package com.hufeng943.timetable.sync

import android.content.Context
import android.content.Intent

object WearProfileIntentRouter {

    fun open(context: Context, target: String) {
        val intent = Intent(context, WearProfileOpenReceiverService::class.java)
            .putExtra("target", target)
        context.startService(intent)
    }
}
