package com.hufeng943.timetable.shared.importexport

import com.hufeng943.timetable.shared.model.Timetable
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.time.Instant

class ImportPreviewTest {
    private fun table(name: String, color: Long = 1, syncId: String? = null) = Timetable(
        semesterName = name,
        createdAt = Instant.fromEpochMilliseconds(0),
        semesterStart = LocalDate(2026, 9, 1),
        color = color,
        syncId = syncId,
    )

    @Test fun distinguishesNewUnchangedAndChangedWithoutReplacingIds() {
        val old = table("Autumn", syncId = "stable")
        val same = old.copy(timetableId = 0, createdAt = Instant.fromEpochMilliseconds(9))
        val changed = old.copy(color = 2)
        val fresh = table("Spring")
        val preview = ImportPreviewBuilder.build(listOf(same, fresh), listOf(old))
        assertEquals(1, preview.newCount)
        assertEquals(1, preview.unchangedCount)
        assertEquals(listOf(fresh), preview.selected(false))
        assertEquals(ImportDisposition.CHANGED, ImportPreviewBuilder.build(listOf(changed), listOf(old)).items.single().disposition)
    }

    @Test fun rejectsDuplicateTimetablesInsideOneFile() {
        assertThrows(IllegalArgumentException::class.java) {
            ImportPreviewBuilder.build(listOf(table("Autumn"), table("Autumn", color = 2)), emptyList())
        }
    }
}
