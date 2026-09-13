package com.hufeng943.timetable.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.resolveDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Clock

object WeeklyTimetableShare {
    private const val WIDTH = 1080
    private const val SIDE = 64f
    private const val TOP = 64f
    private const val DAY_GAP = 26f
    private const val LINE_HEIGHT = 54f

    fun share(context: Context, timetable: Timetable) {
        val bitmap = render(timetable)
        val dir = File(context.cacheDir, "shared_timetables").apply { mkdirs() }
        val file = File(dir, "timetable-week-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "分享本周课表",
            )
        )
    }

    fun render(timetable: Timetable): Bitmap {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val monday = today.minus((today.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber).toLong(), DateTimeUnit.DAY)
        val days = (0..6).map { monday.plus(it, DateTimeUnit.DAY) }
        val occurrences = days.associateWith { timetable.resolveDate(it) }
        val rowLines = days.sumOf { date -> occurrences[date].orEmpty().size.coerceAtLeast(1) }
        val height = (TOP + 150f + rowLines * LINE_HEIGHT + 7 * DAY_GAP + 80f).toInt().coerceAtLeast(900)
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(248, 249, 252))

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 28, 32)
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(92, 96, 104)
            textSize = 28f
        }
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(38, 44, 54)
            textSize = 31f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val coursePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(45, 48, 54)
            textSize = 29f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(130, 134, 142)
            textSize = 27f
        }

        var y = TOP
        canvas.drawText(ellipsize(timetable.semesterName.ifBlank { "本周课表" }, titlePaint, WIDTH - SIDE * 2), SIDE, y + 46f, titlePaint)
        y += 72f
        canvas.drawText("${monday} — ${days.last()}", SIDE, y + 30f, subPaint)
        y += 74f

        for (date in days) {
            val list = occurrences[date].orEmpty().sortedBy { it.startTime }
            val weekday = when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "周一"
                DayOfWeek.TUESDAY -> "周二"
                DayOfWeek.WEDNESDAY -> "周三"
                DayOfWeek.THURSDAY -> "周四"
                DayOfWeek.FRIDAY -> "周五"
                DayOfWeek.SATURDAY -> "周六"
                DayOfWeek.SUNDAY -> "周日"
            }
            canvas.drawText("$weekday · ${date.monthNumber}/${date.dayOfMonth}", SIDE, y + 32f, dayPaint)
            y += 52f
            if (list.isEmpty()) {
                canvas.drawText("无课程", SIDE + 28f, y + 30f, mutedPaint)
                y += LINE_HEIGHT
            } else {
                for (occurrence in list) {
                    val location = occurrence.location?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                    val line = "${occurrence.startTime}–${occurrence.endTime}  ${occurrence.course.name}$location"
                    canvas.drawText(ellipsize(line, coursePaint, WIDTH - SIDE * 2 - 28f), SIDE + 28f, y + 31f, coursePaint)
                    y += LINE_HEIGHT
                }
            }
            y += DAY_GAP
        }

        canvas.drawText("Timetable · 仅显示本周有效课程", SIDE, (height - 42).toFloat(), mutedPaint)
        return bitmap
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val suffix = "…"
        val available = (maxWidth - paint.measureText(suffix)).coerceAtLeast(0f)
        val count = paint.breakText(text, true, available, null).coerceAtLeast(0)
        return text.take(count) + suffix
    }
}
