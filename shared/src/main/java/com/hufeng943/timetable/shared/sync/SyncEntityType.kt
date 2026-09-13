package com.hufeng943.timetable.shared.sync

object SyncEntityType {
    const val TIMETABLE = "TIMETABLE"
    const val COURSE = "COURSE"
    const val TIME_SLOT = "TIME_SLOT"
    const val ACADEMIC_EVENT = "ACADEMIC_EVENT"
}

object SyncOperation {
    const val UPSERT = "UPSERT"
    const val DELETE = "DELETE"
}
