package com.hufeng943.timetable.data

import android.content.Context
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.hufeng943.timetable.shared.importexport.ChinaWearEnvelope
import com.hufeng943.timetable.shared.importexport.ChinaWearPacketCodec
import com.hufeng943.timetable.shared.importexport.ChinaWearProfilePayload
import com.hufeng943.timetable.shared.importexport.ChinaWearProtocol
import com.hufeng943.timetable.shared.importexport.WearTransportMessageProtocol
import com.hufeng943.timetable.sync.ChinaWearBleClient
import com.hufeng943.timetable.sync.LegacyWearIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.TimeUnit

object WearProfileTransfer {
    suspend fun openProfile(context: Context, request: ChinaWearProfilePayload): Boolean = withContext(Dispatchers.IO) {
        val google = runCatching {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == 0 &&
                LegacyWearIo.hasConnectedNodes(context)
        }.getOrDefault(false)
        if (google) return@withContext openWithGoogle(context, request)

        val requestId = UUID.randomUUID().toString()
        val envelope = ChinaWearEnvelope(
            requestId = requestId,
            type = "PROFILE_OPEN_REQUEST",
            timestamp = System.currentTimeMillis(),
            version = ChinaWearProtocol.VERSION,
            payload = Json { encodeDefaults = true }.encodeToString(request),
        )
        val response = ChinaWearBleClient(context).request(ChinaWearPacketCodec.encode(envelope))
        val ack = ChinaWearPacketCodec.decode(response)
        ack.requestId == requestId && ack.type == "PROFILE_OPEN_ACK"
    }

    private fun openWithGoogle(context: Context, request: ChinaWearProfilePayload): Boolean {
        val node = LegacyWearIo.withClient(context) { client ->
            val result = Wearable.NodeApi.getConnectedNodes(client).await(8, TimeUnit.SECONDS)
            if (!result.status.isSuccess) error("无法获取手机节点")
            result.nodes.sortedByDescending(Node::isNearby).firstOrNull() ?: error("未连接手机")
        }
        val payload = Json { encodeDefaults = true }.encodeToString(request).toByteArray(Charsets.UTF_8)
        return LegacyWearIo.sendMessage(context, node.id, WearTransportMessageProtocol.PROFILE_OPEN_PATH, payload)
    }
}
