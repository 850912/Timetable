package com.hufeng943.timetable.sync

import android.content.Context
import android.os.Build
import com.google.android.gms.wearable.Wearable
import com.hufeng943.timetable.shared.importexport.WearBridgeProtocol
import java.util.concurrent.TimeUnit

/**
 * Samsung China bootstrap: the watch deliberately initiates the first Data Layer
 * traffic. Real-device testing showed that this wakes the phone-side Wearable API
 * out of API_UNAVAILABLE(16) on a cold connection.
 */
object WearBridgeBootstrap {
    fun start(context: Context) {
        val appContext = context.applicationContext
        Thread {
            repeat(3) { attempt ->
                val sent = runCatching {
                    LegacyWearIo.withClient(appContext) { client ->
                        val nodes = Wearable.NodeApi.getConnectedNodes(client)
                            .await(10, TimeUnit.SECONDS)
                        if (!nodes.status.isSuccess || nodes.nodes.isEmpty()) return@withClient false
                        val payload = buildString {
                            append("WATCH_HELLO|")
                            append(WearBridgeProtocol.PROTOCOL_VERSION)
                            append('|')
                            append(System.currentTimeMillis())
                            append('|')
                            append(Build.MODEL)
                        }.toByteArray(Charsets.UTF_8)
                        nodes.nodes.any { node ->
                            Wearable.MessageApi.sendMessage(client, node.id, WearBridgeProtocol.HELLO_PATH, payload)
                                .await(10, TimeUnit.SECONDS).status.isSuccess
                        }
                    }
                }.getOrDefault(false)
                if (sent) return@Thread
                if (attempt < 2) Thread.sleep(2_000L)
            }
        }.start()
    }
}
