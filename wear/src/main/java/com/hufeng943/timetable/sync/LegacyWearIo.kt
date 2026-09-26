package com.hufeng943.timetable.sync

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataApi
import com.google.android.gms.wearable.Wearable
import java.io.InputStream
import java.util.concurrent.TimeUnit

internal object LegacyWearIo {
    private const val CONNECT_TIMEOUT_SECONDS = 8L
    private const val OP_TIMEOUT_SECONDS = 10L
    private const val RETRY_DELAY_MS = 2_000L
    private const val CONNECT_ATTEMPTS = 3

    fun <T> withClient(context: Context, block: (GoogleApiClient) -> T): T {
        var lastError: Throwable? = null
        repeat(CONNECT_ATTEMPTS) { attempt ->
            val client = GoogleApiClient.Builder(context.applicationContext)
                .addApi(Wearable.API)
                .build()
            try {
                val result = client.blockingConnect(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (result.isSuccess && client.hasConnectedApi(Wearable.API)) return block(client)
                lastError = IllegalStateException(
                    "Wear OS 连接失败(${result.errorCode}${if (result.errorCode == ConnectionResult.API_UNAVAILABLE) ": API_UNAVAILABLE" else ""})"
                )
            } catch (t: Throwable) {
                lastError = t
            } finally {
                if (client.isConnected || client.isConnecting) client.disconnect()
            }
            if (attempt + 1 < CONNECT_ATTEMPTS) Thread.sleep(RETRY_DELAY_MS)
        }
        throw lastError ?: IllegalStateException("Wear OS 连接失败")
    }

    fun hasConnectedNodes(context: Context): Boolean = withClient(context) { client ->
        val result = Wearable.NodeApi.getConnectedNodes(client).await(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        result.status.isSuccess && result.nodes.isNotEmpty()
    }

    fun localNodeId(context: Context): String = withClient(context) { client ->
        val result = Wearable.NodeApi.getLocalNode(client).await(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!result.status.isSuccess) error("无法获取本机节点(${result.status.statusCode})")
        result.node.id
    }

    fun readAsset(context: Context, asset: Asset): InputStream = withClient(context) { client ->
        val result: DataApi.GetFdForAssetResult = Wearable.DataApi.getFdForAsset(client, asset)
            .await(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!result.status.isSuccess) error("无法读取 Wear Asset(${result.status.statusCode})")
        val bytes = (result.inputStream ?: error("Wear Asset 没有可读输入流")).use { it.readBytes() }
        java.io.ByteArrayInputStream(bytes)
    }

    fun deleteDataItem(context: Context, uri: android.net.Uri) = withClient(context) { client ->
        val result = Wearable.DataApi.deleteDataItems(client, uri).await(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!result.status.isSuccess) error("删除 DataItem 失败(${result.status.statusCode})")
    }

    fun putDataItem(context: Context, request: com.google.android.gms.wearable.PutDataRequest) = withClient(context) { client ->
        val result = Wearable.DataApi.putDataItem(client, request).await(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!result.status.isSuccess) error("发送 DataItem 失败(${result.status.statusCode})")
    }

    fun sendMessage(context: Context, nodeId: String, path: String, payload: ByteArray): Boolean = withClient(context) { client ->
        val result = Wearable.MessageApi.sendMessage(client, nodeId, path, payload)
            .await(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        result.status.isSuccess
    }
}
