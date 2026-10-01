package com.hufeng943.timetable.sync

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataApi
import com.google.android.gms.wearable.Wearable
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Compatibility bridge for the legacy Wearable API used by the verified China-device
 * Data Layer path. Keep the legacy wire/API behavior, but never let its blocking calls
 * execute on the UI thread.
 */
internal object LegacyWearIo {
    private const val MAX_ASSET_BYTES = 2 * 1024 * 1024
    private const val CONNECT_TIMEOUT_SECONDS = 8L
    private const val OP_TIMEOUT_SECONDS = 10L
    private const val RETRY_DELAY_MS = 2_000L
    private const val CONNECT_ATTEMPTS = 3

    suspend fun <T> withClient(context: Context, block: (GoogleApiClient) -> T): T =
        withContext(Dispatchers.IO) {
            var lastError: Throwable? = null
            repeat(CONNECT_ATTEMPTS) { attempt ->
                coroutineContext.ensureActive()
                val client = GoogleApiClient.Builder(context.applicationContext)
                    .addApi(Wearable.API)
                    .build()
                try {
                    val result = client.blockingConnect(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    coroutineContext.ensureActive()
                    if (result.isSuccess && client.hasConnectedApi(Wearable.API)) {
                        return@withContext block(client)
                    }
                    lastError = IllegalStateException(
                        "Wear OS 连接失败(${result.errorCode}${if (result.errorCode == ConnectionResult.API_UNAVAILABLE) ": API_UNAVAILABLE" else ""})"
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    lastError = error
                } finally {
                    if (client.isConnected || client.isConnecting) client.disconnect()
                }
                if (attempt + 1 < CONNECT_ATTEMPTS) delay(RETRY_DELAY_MS)
            }
            throw lastError ?: IllegalStateException("Wear OS 连接失败")
        }

    suspend fun localNodeId(context: Context): String = withClient(context) { client ->
        val result = Wearable.NodeApi.getLocalNode(client).await(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!result.status.isSuccess) error("无法获取本机节点(${result.status.statusCode})")
        result.node.id
    }

    suspend fun readAsset(context: Context, asset: Asset): InputStream = withClient(context) { client ->
        val result: DataApi.GetFdForAssetResult = Wearable.DataApi.getFdForAsset(client, asset)
            .await(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!result.status.isSuccess) error("无法读取 Wear Asset(${result.status.statusCode})")
        val bytes = try {
            (result.inputStream ?: error("Wear Asset 没有可读输入流")).use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_ASSET_BYTES) { "Wear Asset 超过大小限制" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally {
            result.release()
        }
        java.io.ByteArrayInputStream(bytes)
    }

    suspend fun deleteDataItem(context: Context, uri: android.net.Uri) = withClient(context) { client ->
        val result = Wearable.DataApi.deleteDataItems(client, uri).await(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!result.status.isSuccess) error("删除 DataItem 失败(${result.status.statusCode})")
    }

    suspend fun sendMessage(context: Context, nodeId: String, path: String, payload: ByteArray): Boolean =
        withClient(context) { client ->
            val result = Wearable.MessageApi.sendMessage(client, nodeId, path, payload)
                .await(OP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            result.status.isSuccess
        }
}
