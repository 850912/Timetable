package com.hufeng943.timetable.sync

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
/** Build13: package/scheme-aware phone-side profile opener with fallback and diagnostics. */
class WearProfileOpenReceiverService : android.app.Service() {
    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val target = intent?.getStringExtra(EXTRA_TARGET).orEmpty()
        val appUri = intent?.getStringExtra(EXTRA_APP_URI)
        val fallback = intent?.getStringExtra(EXTRA_FALLBACK_URL)
        runCatching { openTarget(target, appUri, fallback) }
            .onFailure { SyncDiagnosticsReporter.recordError(this, "profile_open_failed:$target:${it.message}") }
            .also { stopSelf(startId) }
        return START_NOT_STICKY
    }

    private fun openTarget(target: String, appUri: String?, fallbackUrl: String?) {
        val uri = appUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        val packageName = when (target) {
            "coolapk" -> "com.coolapk.market"
            "douyin" -> "com.ss.android.ugc.aweme"
            else -> null
        }
        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val handler = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (handler != null && (packageName == null || handler.activityInfo?.packageName == packageName)) {
                startActivity(intent)
                SyncDiagnosticsReporter.recordProfile(this, "profile_open_success:$target:app")
                return
            }
        }
        if (!fallbackUrl.isNullOrBlank()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                startActivity(intent)
                SyncDiagnosticsReporter.recordProfile(this, "profile_open_success:$target:fallback")
                return
            }
        }
        throw IllegalStateException("没有可用的 App Scheme 或网页浏览器：$target")
    }

    companion object {
        const val EXTRA_TARGET = "target"
        const val EXTRA_APP_URI = "app_uri"
        const val EXTRA_FALLBACK_URL = "fallback_url"
    }
}
