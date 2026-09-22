
package com.hufeng943.timetable.sync

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LifecycleService

/**
 * Build13: receives profile-open requests from Wear.
 * The actual transport is intentionally abstracted from the UI.
 */
class WearProfileOpenReceiverService : LifecycleService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra("target")) {
            "coolapk" -> open("https://www.coolapk.com")
            "douyin" -> open("https://www.douyin.com")
            "website" -> open("https://github.com")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun open(url: String) {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(view)
    }
}
