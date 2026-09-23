package com.hufeng943.timetable.sync

/**
 * Runtime transport capability snapshot used by Build16 diagnostics.
 * It intentionally describes capabilities rather than inferring a vendor from Build.MANUFACTURER.
 */
data class WearTransportCapability(
    val googleDataLayer: Boolean,
    val chinaBleSupported: Boolean,
    val chinaBlePermissionGranted: Boolean,
    val bluetoothEnabled: Boolean,
    val selected: String,
    val reason: String,
)
