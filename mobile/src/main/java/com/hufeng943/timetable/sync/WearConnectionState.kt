package com.hufeng943.timetable.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Process-local connection signal fed by WearableListenerService peer callbacks. */
object WearConnectionState {
    private val _connected = MutableStateFlow<Boolean?>(null)
    val connected: StateFlow<Boolean?> = _connected

    fun update(value: Boolean) {
        _connected.value = value
    }
}
