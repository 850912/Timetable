package com.hufeng943.timetable.presentation.ui.screens.more.settings.export

import com.hufeng943.timetable.shared.export.ExportTarget

/**
 * Phone-side export target selection state holder.
 */
data class ExportTargetSelector(
    val target: ExportTarget = ExportTarget.PHONE_APP
)
