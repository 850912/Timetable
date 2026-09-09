package com.hufeng943.timetable.probe

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.hufeng943.timetable.sync.LegacyWearIo

class PhoneWearProbeListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearProbe.MESSAGE_PATH -> {
                val body = messageEvent.data.toString(Charsets.UTF_8)
                WearProbe.saveEvent(this, "收到手表 Message：$body · node=${messageEvent.sourceNodeId}")
                Thread {
                    runCatching {
                        LegacyWearIo.sendMessage(
                            this,
                            messageEvent.sourceNodeId,
                            WearProbe.ACK_PATH,
                            "PHONE_ACK|${System.currentTimeMillis()}".toByteArray()
                        )
                    }
                }.start()
            }
            WearProbe.ACK_PATH -> {
                WearProbe.saveEvent(this, "收到手表 ACK：${messageEvent.data.toString(Charsets.UTF_8)}")
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WearProbe.DATA_PATH) {
                WearProbe.saveEvent(this, "收到手表 DataItem：${event.dataItem.uri}")
            }
        }
    }
}
