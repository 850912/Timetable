package com.hufeng943.timetable.sync

import android.content.Context
import android.net.Uri
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Result
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataApi
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataItemBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Compatibility transport for China Wear OS.
 *
 * Keep the production dependency at the current 20.x SDK, but use the
 * GoogleApiClient/Wearable.*Api call path that is known to work on the
 * China Wear OS stack. No 10.2.0 dependency is introduced here.
 */
object LegacyWearableClient {
    private const val CONNECT_TIMEOUT_SECONDS = 8L
    private const val CONNECT_ATTEMPTS = 3
    private const val RETRY_DELAY_MS = 2_000L

    suspend fun <T> withClient(context: Context, block: (GoogleApiClient) -> T): T = withContext(Dispatchers.IO) {
        var last: Throwable? = null
        repeat(CONNECT_ATTEMPTS) { attempt ->
            val client = GoogleApiClient.Builder(context.applicationContext)
                .addApi(Wearable.API)
                .build()
            try {
                val result = client.blockingConnect(CONNECT_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                if (result.isSuccess && client.hasConnectedApi(Wearable.API)) {
                    return@withContext block(client)
                }
                last = IllegalStateException("Wearable API unavailable: ${result.errorCode}")
            } catch (t: Throwable) {
                last = t
            } finally {
                if (client.isConnected || client.isConnecting) client.disconnect()
            }
            if (attempt + 1 < CONNECT_ATTEMPTS) Thread.sleep(RETRY_DELAY_MS)
        }
        throw last ?: IllegalStateException("Wearable API connection failed")
    }

    suspend fun connectedNodes(context: Context): List<Node> = withClient(context) {
        Wearable.NodeApi.getConnectedNodes(it).await().nodes
    }

    suspend fun localNode(context: Context): Node = withClient(context) {
        Wearable.NodeApi.getLocalNode(it).await().node
    }

    suspend fun putDataItem(context: Context, request: PutDataRequest): DataItem = withClient(context) {
        val result = Wearable.DataApi.putDataItem(it, request).await()
        if (!result.status.isSuccess) error("putDataItem failed: ${result.status.statusCode}")
        result.dataItem
    }

    fun putDataItemBlocking(context: Context, request: PutDataRequest): DataItem = runBlockingIo {
        putDataItem(context, request)
    }

    fun deleteDataItemsBlocking(context: Context, uri: Uri) = runBlockingIo {
        withClient(context) { Wearable.DataApi.deleteDataItems(it, uri).await() }
    }

    fun readAssetBlocking(context: Context, asset: Asset): java.io.InputStream = runBlockingIo {
        withClient(context) {
            val result = Wearable.DataApi.getFdForAsset(it, asset).await()
            if (!result.status.isSuccess) error("getFdForAsset failed: ${result.status.statusCode}")
            result.inputStream ?: error("Asset has no input stream")
        }
    }

    private fun <T> runBlockingIo(block: suspend () -> T): T = kotlinx.coroutines.runBlocking(Dispatchers.IO) { block() }
}

private suspend fun <R : Result> PendingResult<R>.await(): R = suspendCancellableCoroutine { continuation ->
    setResultCallback { result ->
        if (continuation.isActive) continuation.resume(result)
    }
}
