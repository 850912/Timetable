package com.hufeng943.timetable.sync

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object ChinaWearBleCapabilities {
    fun isHardwareSupported(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    fun isBluetoothEnabled(context: Context): Boolean =
        context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true

    fun hasRuntimePermission(context: Context): Boolean = hasClientPermission(context)

    fun hasClientPermission(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= 31 ->
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        Build.VERSION.SDK_INT >= 23 ->
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        else -> true
    }

    fun hasServerPermission(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= 31 ->
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        else -> hasClientPermission(context)
    }

    fun isSupported(context: Context): Boolean =
        isHardwareSupported(context) && isBluetoothEnabled(context) && hasClientPermission(context)
}
