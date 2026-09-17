package com.hufeng943.timetable

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.resolveDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlin.math.max

/**
 * Lightweight, on-demand weekly timetable renderer for the phone app.
 * It is intentionally a custom View rather than seven nested RecyclerViews so the
 * expensive schedule surface only exists while the user is looking at it.
 */
class WeeklyScheduleView(
    context: Context,
    private val timetable: Timetable,
    private val weekStart: LocalDate,
) : View(context) {
    private val appContext = context
    private val density = resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolveTextColor()
        textSize = dp(11f)
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolveSecondaryTextColor()
        textSize = dp(9.5f)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(26, 255, 255, 255)
        strokeWidth = dp(0.75f)
    }
    private val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolveTextColor()
        textSize = dp(10.5f)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val hourStart: Int
    private val hourEnd: Int

    init {
        val all = (0..6).flatMap { offset -> timetable.resolveDate(weekStart.plus(offset, DateTimeUnit.DAY)) }
        val minHour = all.minOfOrNull { it.startTime.hour } ?: 8
        val maxHour = all.maxOfOrNull { it.endTime.hour + if (it.endTime.minute > 0) 1 else 0 } ?: 18
        hourStart = minHour.coerceAtMost(8).coerceAtLeast(0)
        hourEnd = max(maxHour, hourStart + 8).coerceAtMost(24)
        minimumWidth = dp(760f).toInt()
        minimumHeight = dp(520f).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = max(minimumWidth, MeasureSpec.getSize(widthMeasureSpec))
        val desiredHeight = max(minimumHeight, dp(((hourEnd - hourStart) * 48 + 58).toFloat()).toInt())
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec), resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val leftGutter = dp(46f)
        val top = dp(48f)
        val usableWidth = width - leftGutter - dp(8f)
        val colWidth = usableWidth / 7f
        val hourHeight = (height - top - dp(10f)) / (hourEnd - hourStart).toFloat()

        val dayNames = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        for (day in 0..6) {
            val date = weekStart.plus(day, DateTimeUnit.DAY)
            val centerX = leftGutter + colWidth * (day + 0.5f)
            canvas.drawText(dayNames[day], centerX, dp(18f), headerPaint)
            canvas.drawText("${date.monthNumber}/${date.dayOfMonth}", centerX, dp(34f), smallPaint.apply { textAlign = Paint.Align.CENTER })
        }
        smallPaint.textAlign = Paint.Align.LEFT

        for (hour in hourStart..hourEnd) {
            val y = top + (hour - hourStart) * hourHeight
            canvas.drawLine(leftGutter, y, width - dp(8f), y, linePaint)
            if (hour < hourEnd) canvas.drawText(String.format("%02d:00", hour), dp(3f), y + dp(4f), smallPaint)
        }
        for (day in 0..7) {
            val x = leftGutter + day * colWidth
            canvas.drawLine(x, top, x, height - dp(10f), linePaint)
        }

        for (day in 0..6) {
            val date = weekStart.plus(day, DateTimeUnit.DAY)
            val occurrences = timetable.resolveDate(date)
            occurrences.forEach { occurrence ->
                val startMinute = occurrence.startTime.hour * 60 + occurrence.startTime.minute
                val endMinute = occurrence.endTime.hour * 60 + occurrence.endTime.minute
                val baseMinute = hourStart * 60
                val startY = top + ((startMinute - baseMinute) / 60f) * hourHeight
                val endY = top + ((endMinute - baseMinute) / 60f) * hourHeight
                val x1 = leftGutter + day * colWidth + dp(3f)
                val x2 = leftGutter + (day + 1) * colWidth - dp(3f)
                val rect = RectF(x1, startY + dp(2f), x2, max(startY + dp(28f), endY - dp(2f)))
                val accent = occurrence.course.color.takeIf { it != -1L }?.toInt() ?: 0xFF6750A4.toInt()
                blockPaint.color = withAlpha(accent, 54)
                canvas.drawRoundRect(rect, dp(10f), dp(10f), blockPaint)
                blockPaint.style = Paint.Style.STROKE
                blockPaint.strokeWidth = dp(1f)
                blockPaint.color = withAlpha(accent, 190)
                canvas.drawRoundRect(rect, dp(10f), dp(10f), blockPaint)
                blockPaint.style = Paint.Style.FILL

                val name = occurrence.course.name.ifBlank { "未命名课程" }
                textPaint.isFakeBoldText = true
                textPaint.color = resolveTextColor()
                canvas.save()
                canvas.clipRect(rect.left + dp(6f), rect.top + dp(4f), rect.right - dp(4f), rect.bottom - dp(4f))
                canvas.drawText(ellipsize(name, colWidth - dp(14f), textPaint), rect.left + dp(6f), rect.top + dp(15f), textPaint)
                textPaint.isFakeBoldText = false
                val timeText = "%02d:%02d".format(occurrence.startTime.hour, occurrence.startTime.minute)
                canvas.drawText(timeText, rect.left + dp(6f), rect.top + dp(29f), smallPaint)
                occurrence.location?.takeIf { it.isNotBlank() && rect.height() > dp(46f) }?.let { location ->
                    canvas.drawText(ellipsize(location, colWidth - dp(14f), smallPaint), rect.left + dp(6f), rect.top + dp(43f), smallPaint)
                }
                canvas.restore()
            }
        }
    }

    private fun ellipsize(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        var result = text
        while (result.length > 1 && paint.measureText("$result…") > maxWidth) result = result.dropLast(1)
        return "$result…"
    }

    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun resolveTextColor(): Int {
        val value = android.util.TypedValue()
        appContext.theme.resolveAttribute(android.R.attr.textColorPrimary, value, true)
        return if (value.resourceId != 0) resources.getColor(value.resourceId, appContext.theme) else Color.WHITE
    }

    private fun resolveSecondaryTextColor(): Int {
        val value = android.util.TypedValue()
        appContext.theme.resolveAttribute(android.R.attr.textColorSecondary, value, true)
        return if (value.resourceId != 0) resources.getColor(value.resourceId, appContext.theme) else Color.LTGRAY
    }
}
