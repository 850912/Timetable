package com.hufeng943.timetable.sync

import android.content.Context
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Chooses transport by runtime environment.
 *
 * Important:
 * This is NOT a device-brand switch.
 * A China ROM and an international ROM on the same hardware may choose
 * different transports.
 */
object TransportSelector {

    enum class Environment {
        GOOGLE_WEAR_STACK,
        COMPATIBILITY_STACK
    }

    fun isGoogleWearAvailable(context: Context): Boolean = hasGoogleWearEnvironment(context)

    fun detect(context: Context): Environment {
        return if (hasGoogleWearEnvironment(context)) {
            Environment.GOOGLE_WEAR_STACK
        } else {
            Environment.COMPATIBILITY_STACK
        }
    }

    private fun hasGoogleWearEnvironment(context: Context): Boolean {
        return runCatching {
            GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == 0 &&
                context.packageManager.getPackageInfo(
                    "com.google.android.gms",
                    0
                ) != null
        }.getOrDefault(false)
    }
}
