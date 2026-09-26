package com.hufeng943.timetable.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Build11-beta phone side receiver boundary.
 *
 * The actual vendor channel is intentionally injected later.
 * This service provides the phone-side lifecycle entry point.
 */
class ChinaWearReceiverService : Service() {

    private lateinit var handler: ChinaWearMessageHandler

    override fun onCreate() {
        super.onCreate()
        handler = ChinaWearMessageHandler(this)
        ChinaWearDiagnosticsLogger.record(
            this,
            "receiver_created"
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun onPacket(type: String, payload: ByteArray): String {
        return handler.handle(type, payload)
    }
}
