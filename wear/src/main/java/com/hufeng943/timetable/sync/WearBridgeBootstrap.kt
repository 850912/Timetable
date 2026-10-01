package com.hufeng943.timetable.sync

import android.content.Context
import android.os.Build
import com.google.android.gms.wearable.Wearable
import com.hufeng943.timetable.shared.importexport.WearBridgeProtocol
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runtime bootstrap for the verified legacy Data Layer compatibility path.
 *
 * Real-device testing on the target China phone/watch pair showed that the watch must
 * initiate the first Data Layer traffic before the phone-side Wearable API reliably
 * leaves API_UNAVAILABLE(16). Keep that handshake and retry cadence unchanged, but run
 * it in an application-owned IO coroutine instead of an unmanaged raw Thread.
 */
object WearBridgeBootstrap {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var bootstrapJob: Job? = null

    fun start(context: Context) {
        val appContext = context.applicationContext
        synchronized(this) {
            if (bootstrapJob?.isActive == true) return
            bootstrapJob = scope.launch {
                repeat(3) { attempt ->
                    if (!isActive) return@launch
                    val sent = try {
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
                                Wearable.MessageApi.sendMessage(
                                    client,
                                    node.id,
                                    WearBridgeProtocol.HELLO_PATH,
                                    payload,
                                ).await(10, TimeUnit.SECONDS).status.isSuccess
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        false
                    }
                    if (sent) return@launch
                    if (attempt < 2) delay(2_000L)
                }
            }
        }
    }
}
