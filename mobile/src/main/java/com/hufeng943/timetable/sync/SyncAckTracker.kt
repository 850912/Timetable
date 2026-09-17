package com.hufeng943.timetable.sync

import com.hufeng943.timetable.shared.sync.SyncAck
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/** Process-local rendezvous between WearOsTransport and the Data Layer listener. */
object SyncAckTracker {
    private val pending = ConcurrentHashMap<String, CompletableDeferred<SyncAck>>()

    fun register(requestId: String): CompletableDeferred<SyncAck> =
        CompletableDeferred<SyncAck>().also { pending[requestId] = it }

    fun complete(ack: SyncAck) {
        pending.remove(ack.requestId)?.complete(ack)
    }

    fun cancel(requestId: String, cause: Throwable? = null) {
        pending.remove(requestId)?.let { deferred ->
            if (cause == null) {
                deferred.cancel()
            } else {
                deferred.completeExceptionally(cause)
            }
        }
    }
}
