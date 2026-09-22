package com.hufeng943.timetable.sync

import android.content.Context
import com.hufeng943.timetable.shared.sync.SyncTransport

/**
 * Chooses transport according to Wear environment.
 *
 * Google Wear stack and China-compatible stack are different runtime
 * environments, not different watch brands.
 */
object SyncTransportProvider {
    fun create(context: Context): List<SyncTransport> {
        return when (TransportSelector.detect(context)) {
            TransportSelector.Environment.GOOGLE_WEAR_STACK ->
                listOf(WearOsTransport(context))
            TransportSelector.Environment.COMPATIBILITY_STACK ->
                listOf(ChinaCompatibleWearTransport(context))
        }
    }
}
