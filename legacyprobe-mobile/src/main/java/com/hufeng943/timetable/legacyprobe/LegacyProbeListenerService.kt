package com.hufeng943.timetable.legacyprobe

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class LegacyProbeListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            LegacyProbe.MESSAGE_PATH -> {
                val body = messageEvent.data.toString(Charsets.UTF_8)
                LegacyProbe.saveEvent(this, "收到对端 Message：$body · node=${messageEvent.sourceNodeId}")
                Thread {
                    val ack = LegacyProbe.sendAck(this, messageEvent.sourceNodeId, ROLE)
                    LegacyProbe.saveEvent(this, "收到 Message 并回 ACK：$body · $ack")
                }.start()
            }
            LegacyProbe.ACK_PATH -> {
                LegacyProbe.saveEvent(this, "收到对端 ACK：${messageEvent.data.toString(Charsets.UTF_8)} · node=${messageEvent.sourceNodeId}")
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == LegacyProbe.DATA_PATH) {
                LegacyProbe.saveEvent(this, "收到对端 DataItem：${event.dataItem.uri}")
            }
        }
    }

    companion object {
        const val ROLE = "PHONE"
    }
}
