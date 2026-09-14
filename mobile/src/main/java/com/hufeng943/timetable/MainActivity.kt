package com.hufeng943.timetable

import android.app.DatePickerDialog
import android.app.AlarmManager
import com.hufeng943.timetable.reminder.ReminderSettings
import com.hufeng943.timetable.reminder.CourseReminderScheduler
import android.widget.CheckBox
import android.text.InputType
import android.os.Build
import android.Manifest
import android.app.TimePickerDialog
import android.graphics.Color
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.HorizontalScrollView
import android.widget.Spinner
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.text.TextUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.importexport.TimetableFileParser
import com.hufeng943.timetable.shared.model.AcademicEvent
import com.hufeng943.timetable.shared.model.AcademicEventType
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.ScheduleOverrideType
import com.hufeng943.timetable.shared.model.ScheduleOverride
import com.hufeng943.timetable.shared.model.ScheduleBatchOperations
import com.hufeng943.timetable.calendar.SystemCalendarSync
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import com.hufeng943.timetable.shared.model.weekNumberFor
import com.hufeng943.timetable.shared.model.dailySummary
import com.hufeng943.timetable.shared.model.resolveDate
import com.hufeng943.timetable.shared.sync.SyncManager
import com.hufeng943.timetable.shared.sync.SyncRecordPayload
import com.hufeng943.timetable.sync.WearOsTransport
import com.hufeng943.timetable.sync.WearConnectionState
import com.hufeng943.timetable.probe.WearProbeActivity
import com.hufeng943.timetable.widget.TodayWidgetProvider
import com.hufeng943.timetable.share.WeeklyTimetableShare
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.hufeng943.timetable.sync.AutoSyncJobService
import android.widget.ProgressBar
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class MainActivity : AppCompatActivity() {
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: TimetableRepository
    private lateinit var syncManager: SyncManager
    private lateinit var wearTransport: WearOsTransport
    private lateinit var timetableContainer: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var connectionStatus: TextView
    private lateinit var syncProgress: ProgressBar
    private var currentTimetables: List<Timetable> = emptyList()
    private var pendingCalendarTimetable: Timetable? = null
    private var pendingCalendarAction: CalendarAction = CalendarAction.SYNC

    private enum class CalendarAction { SYNC, CLEAR }

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importTimetableFile(uri)
    }

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) toast("未授予通知权限，课程提醒不会显示通知")
    }

    private val requestCalendarPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.READ_CALENDAR] == true && result[Manifest.permission.WRITE_CALENDAR] == true
        val table = pendingCalendarTimetable
        val action = pendingCalendarAction
        pendingCalendarTimetable = null
        pendingCalendarAction = CalendarAction.SYNC
        if (granted && table != null) {
            when (action) {
                CalendarAction.SYNC -> syncTimetableToCalendar(table)
                CalendarAction.CLEAR -> clearTimetableFromCalendar(table)
            }
        } else if (!granted) {
            toast("需要日历读写权限才能操作系统日历")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        repository = TimetableDatabaseProvider.repository(this)
        wearTransport = WearOsTransport(this)
        syncManager = SyncManager(listOf(wearTransport))
        timetableContainer = findViewById(R.id.timetableContainer)
        emptyText = findViewById(R.id.emptyText)
        connectionStatus = findViewById(R.id.connectionStatus)
        syncProgress = findViewById(R.id.syncProgress)
        connectionStatus.setOnClickListener {
            uiScope.launch {
                val reset = withContext(Dispatchers.IO) {
                    TimetableDatabaseProvider.database(this@MainActivity).syncRecordDao().retryFailed()
                }
                if (reset > 0) {
                    toast("已重新加入 $reset 条失败变更")
                    AutoSyncJobService.scheduleNow(this@MainActivity)
                }
            }
        }

        findViewById<MaterialButton>(R.id.buttonManualCreate).setOnClickListener {
            showCreateTimetableDialog()
        }
        findViewById<MaterialButton>(R.id.buttonQuickCreate).setOnClickListener {
            showQuickCreateDialog()
        }
        findViewById<MaterialButton>(R.id.buttonImport).setOnClickListener {
            openDocument.launch(arrayOf("application/json", "text/csv", "text/calendar", "text/*"))
        }
        findViewById<MaterialButton>(R.id.buttonSyncAll).setOnClickListener {
            syncToWatch(forceFullSnapshot = true)
        }
        findViewById<MaterialButton>(R.id.buttonSyncStatus).setOnClickListener {
            showSyncStatusDialog()
        }
        findViewById<MaterialButton>(R.id.buttonReminderSettings).setOnClickListener {
            showReminderSettingsDialog()
        }
        findViewById<MaterialButton>(R.id.buttonWearProbe).setOnClickListener {
            startActivity(Intent(this, WearProbeActivity::class.java))
        }

        observeTimetables()
        monitorWearConnection()
    }

    private fun observeTimetables() {
        uiScope.launch {
            repository.getAllTimetables().collectLatest { timetables ->
                currentTimetables = timetables.sortedByDescending { it.createdAt }
                renderTimetables(currentTimetables)
                TodayWidgetProvider.requestRefresh(this@MainActivity)
                withContext(Dispatchers.Default) {
                    CourseReminderScheduler.schedule(this@MainActivity, currentTimetables)
                }
                AutoSyncJobService.scheduleNow(this@MainActivity)
            }
        }
    }

    private fun renderTimetables(timetables: List<Timetable>) {
        timetableContainer.removeAllViews()
        emptyText.visibility = if (timetables.isEmpty()) View.VISIBLE else View.GONE

        timetables.forEach { timetable ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(12))
                background = GradientDrawable().apply {
                    cornerRadius = dp(24).toFloat()
                    setColor(resolveSurfaceColor())
                    setStroke(dp(1), 0x22000000)
                }
            }
            card.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }

            val today = Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
            val summary = timetable.dailySummary(today)

            card.addView(TextView(this).apply {
                text = timetable.semesterName.ifBlank { "未命名课表" }
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            card.addView(TextView(this).apply {
                val end = timetable.semesterEnd?.toString() ?: "长期"
                val currentWeek = timetable.weekNumberFor(today)
                val totalWeeks = timetable.semesterEnd?.let { timetable.weekNumberFor(it) }
                val week = when {
                    currentWeek != null && totalWeeks != null && totalWeeks >= currentWeek -> " · 第${currentWeek}/${totalWeeks}周"
                    currentWeek != null -> " · 第${currentWeek}周"
                    else -> ""
                }
                text = "${timetable.semesterStart} ～ $end · ${timetable.allCourses.size} 门课程$week"
                setPadding(0, dp(5), 0, dp(3))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            card.addView(TextView(this).apply {
                text = if (summary.courseCount == 0) {
                    "今日无课"
                } else {
                    val free = summary.freeMinutesBetweenCourses.takeIf { it > 0 }?.let { " · 课间 ${formatMinutes(it)}" }.orEmpty()
                    "今日 ${summary.courseCount} 节 · ${summary.firstStart}–${summary.lastEnd}$free"
                }
                setPadding(0, 0, 0, dp(8))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })

            val todayOccurrences = timetable.resolveDate(today)
            if (todayOccurrences.isNotEmpty()) {
                card.addView(TextView(this).apply {
                    text = "今日课表"
                    textSize = 15f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, dp(4), 0, dp(6))
                })
                val nowTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
                todayOccurrences.forEach { occurrence ->
                    val isCurrent = nowTime >= occurrence.startTime && nowTime < occurrence.endTime
                    val accent = occurrence.course.color.takeIf { it != -1L }?.toInt() ?: resolvePrimaryColor()
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(dp(10), dp(9), dp(10), dp(9))
                        background = GradientDrawable().apply {
                            cornerRadius = dp(18).toFloat()
                            setColor(withAlpha(accent, if (isCurrent) 46 else 22))
                            setStroke(dp(if (isCurrent) 2 else 1), withAlpha(accent, if (isCurrent) 220 else 92))
                        }
                    }
                    row.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(7) }
                    row.addView(View(this).apply {
                        background = GradientDrawable().apply {
                            cornerRadius = dp(99).toFloat()
                            setColor(accent)
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                            marginEnd = dp(10)
                        }
                    })
                    row.addView(LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        addView(TextView(this@MainActivity).apply {
                            text = occurrence.course.name.ifBlank { "未命名课程" }
                            textSize = if (isCurrent) 17f else 16f
                            setTypeface(typeface, if (isCurrent) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                            maxLines = 2
                            ellipsize = TextUtils.TruncateAt.END
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = buildString {
                                append("${occurrence.startTime}–${occurrence.endTime}")
                                occurrence.location?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                                if (isCurrent) append(" · 上课中")
                            }
                            textSize = 13f
                            setPadding(0, dp(2), 0, 0)
                            maxLines = 2
                            ellipsize = TextUtils.TruncateAt.END
                        })
                    })
                    row.setOnClickListener { showCourseActionsDialog(timetable, occurrence.course) }
                    card.addView(row)
                }
            }

            card.addView(TextView(this).apply {
                text = "全部课程"
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dp(6), 0, dp(2))
            })

            timetable.allCourses.forEach { course ->
                val slotText = course.timeSlots.joinToString("；") { slot ->
                    val day = slot.dayOfWeek?.let(::dayLabel) ?: "未定"
                    val startTime = slot.startTime?.toString() ?: "--:--"
                    val endTime = slot.endTime?.toString() ?: "--:--"
                    val overrideSuffix = if (slot.overrides.isNotEmpty()) " · ${slot.overrides.size}个日期例外" else ""
                    "$day $startTime-$endTime$overrideSuffix"
                }.ifBlank { "暂无时间" }
                card.addView(TextView(this).apply {
                    text = "• ${course.name}  $slotText" +
                        listOfNotNull(course.location, course.teacher).takeIf { it.isNotEmpty() }
                            ?.joinToString(prefix = "  （", postfix = "）", separator = " / ").orEmpty()
                    setPadding(0, dp(6), 0, dp(6))
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { showCourseActionsDialog(timetable, course) }
                    setOnLongClickListener {
                        showQuickCourseActionsDialog(timetable, course)
                        true
                    }
                })
            }

            val upcomingEvents = timetable.events
                .filter { !it.completed && it.date >= today }
                .sortedWith(compareBy<AcademicEvent> { it.date }.thenBy { it.time })
            if (upcomingEvents.isNotEmpty()) {
                card.addView(TextView(this).apply {
                    text = "近期待办 · ${upcomingEvents.size}"
                    textSize = 15f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, dp(9), 0, dp(2))
                })
                upcomingEvents.take(3).forEach { event ->
                    card.addView(TextView(this).apply {
                        val whenText = buildString {
                            append(event.date)
                            event.time?.let { append(" $it") }
                        }
                        text = "${eventTypeLabel(event.type)} · ${event.title} · $whenText"
                        setPadding(0, dp(4), 0, dp(4))
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                        isClickable = true
                        isFocusable = true
                        setOnClickListener { showAcademicEventActionsDialog(timetable, event) }
                    })
                }
                if (upcomingEvents.size > 3) {
                    card.addView(TextView(this).apply {
                        text = "另有 ${upcomingEvents.size - 3} 项待办"
                        setPadding(0, dp(2), 0, dp(4))
                    })
                }
            }

            card.addView(TextView(this).apply {
                text = "提示：长按课程可快速处理今天的停课/调课"
                textSize = 12f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, dp(5), 0, 0)
            })

            val quickScheduleActions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(8), 0, 0)
            }
            quickScheduleActions.addView(actionButton("批量调时/停课") { showBatchScheduleDialog(timetable) })
            card.addView(quickScheduleActions)

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(4), 0, 0)
            }
            actions.addView(actionButton("添加课程") { showAddCourseDialog(timetable) })
            actions.addView(actionButton("同步") { syncToWatch(forceFullSnapshot = true) })
            actions.addView(actionButton("删除") { confirmDelete(timetable) })
            card.addView(actions)
            val moreActions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(4), 0, 0)
            }
            moreActions.addView(actionButton("日期例外") { showScheduleOverrideDialog(timetable) })
            moreActions.addView(actionButton("复制课表") { duplicateTimetable(timetable) })
            card.addView(moreActions)
            val calendarActions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(4), 0, 0)
            }
            calendarActions.addView(actionButton("同步日历") { requestCalendarSync(timetable) })
            calendarActions.addView(actionButton("清除日历") { requestCalendarClear(timetable) })
            card.addView(calendarActions)
            val studentActions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(4), 0, 0)
            }
            studentActions.addView(actionButton("待办管理") { showAcademicEventManagerDialog(timetable) })
            studentActions.addView(actionButton("本周课表") { showWeeklyScheduleDialog(timetable) })
            card.addView(studentActions)
            val shareActions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(4), 0, 0)
            }
            shareActions.addView(actionButton("分享本周课表") {
                runCatching { WeeklyTimetableShare.share(this@MainActivity, timetable) }
                    .onFailure { toast(it.message ?: "生成分享图片失败") }
            })
            card.addView(shareActions)
            timetableContainer.addView(card)
        }
    }

    private fun showWeeklyScheduleDialog(timetable: Timetable) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val mondayOffset = (today.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber).mod(7)
        val weekStart = today.minus(mondayOffset.toLong(), DateTimeUnit.DAY)
        val schedule = WeeklyScheduleView(this, timetable, weekStart).apply {
            layoutParams = ViewGroup.LayoutParams(dp(760), dp(560))
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        val scroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(schedule)
        }
        AlertDialog.Builder(this)
            .setTitle("本周课表 · ${weekStart.monthNumber}/${weekStart.dayOfMonth}")
            .setView(scroller)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun showSyncStatusDialog() {
        uiScope.launch {
            val dao = TimetableDatabaseProvider.database(this@MainActivity).syncRecordDao()
            val connected = WearConnectionState.connected.value
                ?: withContext(Dispatchers.IO) { wearTransport.isAvailable() }
            val pending = withContext(Dispatchers.IO) { dao.pendingCount() }
            val failed = withContext(Dispatchers.IO) { dao.permanentlyFailedCount() }
            val stateText = if (connected) "已连接" else "未连接"
            val message = buildString {
                append("手表连接：$stateText\n")
                append("待同步变更：$pending\n")
                append("失败变更：$failed\n")
                append("传输策略：现代 Wearable + 中国区 Legacy fallback\n")
                append("自动同步：已开启")
            }
            AlertDialog.Builder(this@MainActivity)
                .setTitle("同步状态中心")
                .setMessage(message)
                .setNeutralButton("通信诊断") { _, _ -> startActivity(Intent(this@MainActivity, WearProbeActivity::class.java)) }
                .setNegativeButton("关闭", null)
                .setPositiveButton("立即同步") { _, _ -> syncToWatch(forceFullSnapshot = true) }
                .show()
        }
    }

    private fun actionButton(label: String, action: () -> Unit): MaterialButton =
        MaterialButton(this).apply {
            text = label
            isAllCaps = false
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            minimumWidth = 0
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(4)
            }
            setOnClickListener { action() }
        }

    private fun showCreateTimetableDialog() {
        val name = inputField("课表名称", "例如：2026 秋季学期")
        val start = inputField("开始日期", "YYYY-MM-DD").apply { editText?.setText(todayString()) }
        val end = inputField("结束日期（可留空）", "YYYY-MM-DD")
        start.editText?.setOnClickListener { showDatePicker(start.editText ?: return@setOnClickListener) }
        end.editText?.setOnClickListener { showDatePicker(end.editText ?: return@setOnClickListener) }
        start.editText?.isFocusable = false
        end.editText?.isFocusable = false

        val content = dialogColumn(name, start, end)
        val dialog = AlertDialog.Builder(this)
            .setTitle("手动创建课表")
            .setView(content)
            .setNegativeButton("取消", null)
            .setPositiveButton("创建", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                runCatching {
                    val semesterName = name.text().ifBlank { throw IllegalArgumentException("请输入课表名称") }
                    val startDate = LocalDate.parse(start.text())
                    val endDate = end.text().takeIf { it.isNotBlank() }?.let(LocalDate::parse)
                    if (endDate != null && endDate < startDate) {
                        throw IllegalArgumentException("结束日期不能早于开始日期")
                    }
                    Timetable(
                        semesterName = semesterName,
                        createdAt = Clock.System.now(),
                        semesterStart = startDate,
                        semesterEnd = endDate
                    )
                }.onSuccess { timetable ->
                    uiScope.launch {
                        withContext(Dispatchers.IO) { repository.upsertTimetable(timetable) }
                        dialog.dismiss()
                        toast("课表已创建，可继续添加课程")
                    }
                }.onFailure { toast(it.message ?: "创建失败") }
            }
        }
        dialog.show()
    }

    private fun showAddCourseDialog(timetable: Timetable) {
        val name = autocompleteField("课程名称", "例如：高等数学", courseSuggestions())
        val location = autocompleteField("地点（可留空）", "例如：教学楼 A101", locationSuggestions())
        val teacher = autocompleteField("教师（可留空）", "例如：张老师", teacherSuggestions())
        val start = inputField("开始时间", "08:00").apply { editText?.setText("08:00") }
        val end = inputField("结束时间", "09:40").apply { editText?.setText("09:40") }
        start.editText?.isFocusable = false
        end.editText?.isFocusable = false
        start.editText?.setOnClickListener { showTimePicker(start.editText ?: return@setOnClickListener) }
        end.editText?.setOnClickListener { showTimePicker(end.editText ?: return@setOnClickListener) }

        val daySpinner = spinner(listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日"))
        val recurrenceSpinner = spinner(listOf("每周", "单周", "双周"))
        val dayField = labeled("星期", daySpinner)
        val recurrenceField = labeled("重复", recurrenceSpinner)

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val rangeStart = maxOf(today, timetable.semesterStart)
        val rangeEnd = minOf(
            timetable.semesterEnd ?: LocalDate.fromEpochDays(rangeStart.toEpochDays() + 83),
            LocalDate.fromEpochDays(rangeStart.toEpochDays() + 83),
        )
        val candidateDates = buildList {
            var date = rangeStart
            while (date <= rangeEnd) {
                add(date)
                date = LocalDate.fromEpochDays(date.toEpochDays() + 1)
            }
        }
        val selectedDates = linkedSetOf<LocalDate>()
        lateinit var dateButton: MaterialButton
        fun refreshDateMode() {
            val usingDates = selectedDates.isNotEmpty()
            dateButton.text = if (usingDates) {
                "日期 · 已选 ${selectedDates.size} 天"
            } else {
                "日期 · 未指定（点按可多选）"
            }
            dayField.visibility = if (usingDates) View.GONE else View.VISIBLE
            recurrenceField.visibility = if (usingDates) View.GONE else View.VISIBLE
        }
        dateButton = MaterialButton(this).apply {
            isAllCaps = false
            setOnClickListener {
                if (candidateDates.isEmpty()) {
                    toast("当前课表没有可选择的日期")
                    return@setOnClickListener
                }
                val labels = candidateDates.map { "${it.monthNumber}/${it.dayOfMonth} · ${dayLabel(it.dayOfWeek)}" }.toTypedArray()
                val checked = candidateDates.map { it in selectedDates }.toBooleanArray()
                val picker = AlertDialog.Builder(this@MainActivity)
                    .setTitle("选择日期 · 可多选")
                    .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                        val date = candidateDates[which]
                        if (isChecked) selectedDates += date else selectedDates -= date
                    }
                    .setNegativeButton("清空") { _, _ ->
                        selectedDates.clear()
                        refreshDateMode()
                    }
                    .setPositiveButton("完成") { _, _ -> refreshDateMode() }
                    .create()
                picker.setOnDismissListener { refreshDateMode() }
                picker.show()
            }
        }
        refreshDateMode()

        val content = dialogColumn(
            name, location, teacher,
            dateButton,
            dayField,
            start, end,
            recurrenceField
        )

        val dialog = AlertDialog.Builder(this)
            .setTitle("添加课程 · ${timetable.semesterName}")
            .setView(content)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                runCatching {
                    val courseName = name.text().ifBlank { throw IllegalArgumentException("请输入课程名称") }
                    val startTime = runCatching { LocalTime.parse(start.text()) }
                        .getOrElse { throw IllegalArgumentException("开始时间格式错误") }
                    val endTime = runCatching { LocalTime.parse(end.text()) }
                        .getOrElse { throw IllegalArgumentException("结束时间格式错误") }
                    if (endTime <= startTime) throw IllegalArgumentException("结束时间必须晚于开始时间")
                    val slot = if (selectedDates.isNotEmpty()) {
                        ScheduleBatchOperations.dateOnlySlot(
                            dates = selectedDates.toList(),
                            startTime = startTime,
                            endTime = endTime,
                            location = location.text().ifBlank { null },
                        )
                    } else {
                        TimeSlot(
                            startTime = startTime,
                            endTime = endTime,
                            dayOfWeek = DayOfWeek.entries[daySpinner.selectedItemPosition],
                            recurrence = WeekPattern.entries[recurrenceSpinner.selectedItemPosition]
                        )
                    }
                    Course(
                        name = courseName,
                        location = location.text().ifBlank { null },
                        teacher = teacher.text().ifBlank { null },
                        timeSlots = listOf(slot)
                    )
                }.onSuccess { course ->
                    uiScope.launch {
                        withContext(Dispatchers.IO) {
                            val courseId = repository.upsertCourse(course, timetable.timetableId)
                            course.timeSlots.firstOrNull()?.let { slot ->
                                repository.upsertTimeSlot(slot, courseId)
                            } ?: throw IllegalStateException("课程时间不能为空")
                        }
                        dialog.dismiss()
                        toast("课程已添加")
                    }
                }.onFailure { toast(it.message ?: "保存失败") }
            }
        }
        dialog.show()
    }

    private fun showQuickCreateDialog() {
        val name = inputField("课表名称", "例如：2026 秋季学期")
        val start = inputField("开始日期", "YYYY-MM-DD").apply { editText?.setText(todayString()) }
        val end = inputField("结束日期（可留空）", "YYYY-MM-DD")
        start.editText?.isFocusable = false
        end.editText?.isFocusable = false
        start.editText?.setOnClickListener { showDatePicker(start.editText ?: return@setOnClickListener) }
        end.editText?.setOnClickListener { showDatePicker(end.editText ?: return@setOnClickListener) }

        val lines = TextInputLayout(this).apply {
            hint = "每行：课程|星期|时间|地点|教师|重复"
            addView(TextInputEditText(context).apply {
                minLines = 7
                maxLines = 12
                setText("高等数学|周一|08:00-09:40|A101|张老师|每周\n大学英语|周三|10:00-11:40|B203|李老师|单周")
            })
        }
        val help = TextView(this).apply {
            text = "支持：周一~周日；重复可填 每周/单周/双周。地点、教师、重复可省略。"
            setPadding(0, dp(4), 0, dp(8))
        }
        val content = dialogColumn(name, start, end, help, lines)
        val dialog = AlertDialog.Builder(this)
            .setTitle("快速文本创建")
            .setView(content)
            .setNegativeButton("取消", null)
            .setPositiveButton("解析并创建", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                runCatching {
                    val semesterName = name.text().ifBlank { throw IllegalArgumentException("请输入课表名称") }
                    val startDate = LocalDate.parse(start.text())
                    val endDate = end.text().takeIf { it.isNotBlank() }?.let(LocalDate::parse)
                    if (endDate != null && endDate < startDate) throw IllegalArgumentException("结束日期不能早于开始日期")
                    val courses = parseQuickCourses(lines.editText?.text?.toString().orEmpty())
                    if (courses.isEmpty()) throw IllegalArgumentException("没有解析到课程")
                    Timetable(
                        semesterName = semesterName,
                        createdAt = Clock.System.now(),
                        semesterStart = startDate,
                        semesterEnd = endDate,
                        allCourses = courses
                    )
                }.onSuccess { timetable ->
                    uiScope.launch {
                        withContext(Dispatchers.IO) { insertWholeTimetable(timetable) }
                        dialog.dismiss()
                        toast("已创建 ${timetable.allCourses.size} 门课程")
                    }
                }.onFailure { toast(it.message ?: "解析失败") }
            }
        }
        dialog.show()
    }

    private fun parseQuickCourses(text: String): List<Course> {
        val rows = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapIndexed { index, line ->
                val parts = line.split('|').map { it.trim() }
                if (parts.size < 3) throw IllegalArgumentException("第 ${index + 1} 行格式不足，至少需要 课程|星期|时间")
                val times = parts[2].split('-', '～', '~').map { it.trim() }
                if (times.size != 2) throw IllegalArgumentException("第 ${index + 1} 行时间格式应为 08:00-09:40")
                val startTime = LocalTime.parse(times[0])
                val endTime = LocalTime.parse(times[1])
                if (endTime <= startTime) throw IllegalArgumentException("第 ${index + 1} 行结束时间必须晚于开始时间")
                Course(
                    name = parts[0].ifBlank { throw IllegalArgumentException("第 ${index + 1} 行课程名为空") },
                    location = parts.getOrNull(3)?.ifBlank { null },
                    teacher = parts.getOrNull(4)?.ifBlank { null },
                    timeSlots = listOf(
                        TimeSlot(
                            dayOfWeek = parseDay(parts[1]),
                            startTime = startTime,
                            endTime = endTime,
                            recurrence = parseRecurrence(parts.getOrNull(5))
                        )
                    )
                )
            }.toList()

        // Identical course metadata on multiple lines means one course with multiple time slots.
        return rows.groupBy { Triple(it.name, it.location, it.teacher) }
            .values
            .mapNotNull { sameCourseRows ->
                sameCourseRows.firstOrNull()?.copy(
                    timeSlots = sameCourseRows.flatMap { it.timeSlots }
                )
            }
    }

    private fun parseDay(value: String): DayOfWeek = when (value.trim().lowercase()) {
        "周一", "星期一", "一", "mon", "monday" -> DayOfWeek.MONDAY
        "周二", "星期二", "二", "tue", "tuesday" -> DayOfWeek.TUESDAY
        "周三", "星期三", "三", "wed", "wednesday" -> DayOfWeek.WEDNESDAY
        "周四", "星期四", "四", "thu", "thursday" -> DayOfWeek.THURSDAY
        "周五", "星期五", "五", "fri", "friday" -> DayOfWeek.FRIDAY
        "周六", "星期六", "六", "sat", "saturday" -> DayOfWeek.SATURDAY
        "周日", "周天", "星期日", "星期天", "日", "天", "sun", "sunday" -> DayOfWeek.SUNDAY
        else -> throw IllegalArgumentException("无法识别星期：$value")
    }

    private fun parseRecurrence(value: String?): WeekPattern = when (value?.trim()?.lowercase()) {
        null, "", "每周", "every", "weekly" -> WeekPattern.EVERY_WEEK
        "单周", "odd", "odd_week" -> WeekPattern.ODD_WEEK
        "双周", "even", "even_week" -> WeekPattern.EVEN_WEEK
        "仅指定日期", "date_only" -> WeekPattern.DATE_ONLY
        else -> throw IllegalArgumentException("无法识别重复方式：$value")
    }

    private suspend fun insertWholeTimetable(timetable: Timetable) {
        val timetableId = repository.upsertTimetable(timetable.copy(timetableId = 0))
        timetable.allCourses.forEach { course ->
            val courseId = repository.upsertCourse(course.copy(id = 0), timetableId)
            course.timeSlots.forEach { slot ->
                repository.upsertTimeSlot(slot.copy(id = 0), courseId)
            }
        }
        timetable.events.forEach { event ->
            repository.upsertAcademicEvent(event.copy(id = 0), timetableId)
        }
    }

    private fun importTimetableFile(uri: Uri) {
        uiScope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("无法读取所选文件")
                }
                val timetables = withContext(Dispatchers.Default) { TimetableFileParser.parse(bytes) }
                withContext(Dispatchers.IO) {
                    TimetableDatabaseProvider.importService(this@MainActivity).importAtomic(timetables)
                    val snapshot = repository.getAllTimetables().first()
                    enqueueSnapshotForIncrementalSync(snapshot)
                }
                timetables.size
            }.onSuccess {
                toast("成功导入 $it 个课表，已加入自动同步队列")
                AutoSyncJobService.scheduleNow(this@MainActivity)
            }.onFailure { toast(it.message ?: "导入失败") }
        }
    }

    private fun syncToWatch(forceFullSnapshot: Boolean = false) {
        uiScope.launch {
            syncProgress.visibility = View.VISIBLE
            connectionStatus.text = if (forceFullSnapshot) "正在完整同步课表…" else "正在同步课表变更…"
            val result = withContext(Dispatchers.IO) {
                if (forceFullSnapshot) {
                    wearTransport.sendTimetablesSnapshot(currentTimetables)
                } else {
                    val db = TimetableDatabaseProvider.database(this@MainActivity)
                    val records = db.syncRecordDao().pending()
                        .map { SyncRecordPayload(it.id, it.entityId, it.entityType, it.operation, it.revision, it.updatedAt, it.deviceId, it.payloadJson) }
                    val syncResult = syncManager.syncRecords(records)
                    if (syncResult is com.hufeng943.timetable.shared.sync.SyncResult.Failed && records.isNotEmpty()) {
                        db.syncRecordDao().markFailed(
                            records.map { it.sourceRecordId },
                            System.currentTimeMillis(),
                            syncResult.message
                        )
                    }
                    syncResult
                }
            }
            syncProgress.visibility = View.GONE
            when (result) {
                com.hufeng943.timetable.shared.sync.SyncResult.Success -> {
                    connectionStatus.text = "手表已连接 · 同步请求已发送"
                    toast(if (forceFullSnapshot) "完整课表已发送到手表" else "同步请求已发送，等待手表确认")
                }
                is com.hufeng943.timetable.shared.sync.SyncResult.Failed -> {
                    connectionStatus.text = "同步失败 · ${result.message}"
                    toast(result.message)
                }
            }
        }
    }

    private fun monitorWearConnection() {
        val dao = TimetableDatabaseProvider.database(this).syncRecordDao()
        uiScope.launch {
            // Prime the process-local peer state once. Further changes are delivered by
            // WearableListenerService callbacks instead of a permanent 10-second poll.
            WearConnectionState.update(withContext(Dispatchers.IO) { wearTransport.isAvailable() })
            combine(
                WearConnectionState.connected,
                dao.observePendingCount(),
                dao.observePermanentlyFailedCount(),
            ) { connected, pendingCount, failedCount -> Triple(connected, pendingCount, failedCount) }
                .collectLatest { (connected, pendingCount, failedCount) ->
                    connectionStatus.text = when {
                        failedCount > 0 -> "${if (connected == true) "手表已连接" else "手表未连接"} · $failedCount 条变更同步失败，可手动重试"
                        connected == true && pendingCount > 0 -> "手表已连接 · 有 $pendingCount 条变更等待自动同步"
                        connected == true -> "手表已连接 · 自动同步已开启"
                        connected == false -> "手表未连接 · 将在连接后自动重试"
                        else -> "正在检测手表连接…"
                    }
                    if (connected == true && pendingCount > 0) {
                        AutoSyncJobService.scheduleNow(this@MainActivity)
                    }
                }
        }
    }

    private suspend fun enqueueSnapshotForIncrementalSync(timetables: List<Timetable>) {
        timetables.forEach { timetable ->
            val timetableId = repository.upsertTimetable(timetable)
            timetable.allCourses.forEach { course ->
                val courseId = repository.upsertCourse(course, timetableId)
                course.timeSlots.forEach { slot ->
                    repository.upsertTimeSlot(slot, courseId)
                }
            }
            timetable.events.forEach { event -> repository.upsertAcademicEvent(event, timetableId) }
        }
    }

    private fun showQuickCourseActionsDialog(timetable: Timetable, course: Course) {
        val today = Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        val hasToday = timetable.resolveDate(today).any { it.course.id == course.id }
        val options = arrayOf("今天停课", "今日调课 / 换教室", "恢复今天")
        AlertDialog.Builder(this)
            .setTitle(course.name.ifBlank { "课程" })
            .setMessage(if (hasToday) "快速处理今天的课程" else "今天没有这门课；仍可进入日期例外手动调整")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> applyTodayCourseOverride(timetable, course, cancel = true)
                    1 -> showScheduleOverrideDialog(timetable, course.id, ScheduleOverrideType.MODIFIED)
                    2 -> applyTodayCourseOverride(timetable, course, cancel = false)
                }
            }.show()
    }

    private fun applyTodayCourseOverride(timetable: Timetable, course: Course, cancel: Boolean) {
        val today = Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        val matchingSlotIds = timetable.resolveDate(today)
            .filter { it.course.id == course.id }
            .map { it.timeSlot.id }
            .toSet()
        val targetSlots = if (cancel) {
            course.timeSlots.filter { it.id in matchingSlotIds }
        } else {
            course.timeSlots.filter { slot -> slot.overrides.any { it.date == today } }
        }
        if (targetSlots.isEmpty()) {
            toast(if (cancel) "今天没有可停的课时" else "今天没有需要恢复的调整")
            return
        }
        uiScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    targetSlots.forEach { slot ->
                        val updated = if (cancel) {
                            ScheduleBatchOperations.cancelDates(slot, listOf(today))
                        } else {
                            ScheduleBatchOperations.clearDates(slot, listOf(today))
                        }
                        repository.upsertTimeSlot(updated, course.id)
                    }
                }
            }.onSuccess {
                toast(if (cancel) "已将今天的 ${targetSlots.size} 个课时设为停课" else "已恢复今天的课程安排")
            }.onFailure { toast(it.message ?: "保存失败") }
        }
    }

    private fun showBatchScheduleDialog(timetable: Timetable) {
        if (timetable.allCourses.none { it.timeSlots.isNotEmpty() }) {
            toast("请先添加课程和课时")
            return
        }
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val from = inputField("生效开始日期", "YYYY-MM-DD").apply { editText?.setText(maxOf(today, timetable.semesterStart).toString()) }
        val to = inputField("生效结束日期", "YYYY-MM-DD").apply { editText?.setText((timetable.semesterEnd ?: LocalDate.fromEpochDays(today.toEpochDays() + 30)).toString()) }
        from.editText?.isFocusable = false
        to.editText?.isFocusable = false
        from.editText?.setOnClickListener { showDatePicker(from.editText ?: return@setOnClickListener) }
        to.editText?.setOnClickListener { showDatePicker(to.editText ?: return@setOnClickListener) }

        val courseSpinner = spinner(listOf("全部课程") + timetable.allCourses.map { it.name.ifBlank { "未命名课程" } })
        val actionSpinner = spinner(listOf("统一后移/前移", "停课", "恢复正常"))
        val offset = inputField("移动分钟（前移填负数）", "例如：10 或 -15").apply {
            editText?.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            editText?.setText("10")
        }
        val useWindow = CheckBox(this).apply { text = "只处理与指定时间段重叠的课时" }
        val windowStart = inputField("时间段开始", "08:00").apply { editText?.setText("08:00") }
        val windowEnd = inputField("时间段结束", "18:00").apply { editText?.setText("18:00") }
        windowStart.editText?.isFocusable = false
        windowEnd.editText?.isFocusable = false
        windowStart.editText?.setOnClickListener { showTimePicker(windowStart.editText ?: return@setOnClickListener) }
        windowEnd.editText?.setOnClickListener { showTimePicker(windowEnd.editText ?: return@setOnClickListener) }
        fun updateVisibility() {
            offset.visibility = if (actionSpinner.selectedItemPosition == 0) View.VISIBLE else View.GONE
            val visible = if (useWindow.isChecked) View.VISIBLE else View.GONE
            windowStart.visibility = visible
            windowEnd.visibility = visible
        }
        actionSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = updateVisibility()
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        useWindow.setOnCheckedChangeListener { _, _ -> updateVisibility() }
        updateVisibility()

        val dialog = AlertDialog.Builder(this)
            .setTitle("批量调整课程")
            .setView(dialogColumn(labeled("范围", courseSpinner), labeled("操作", actionSpinner), from, to, offset, useWindow, windowStart, windowEnd))
            .setNegativeButton("取消", null)
            .setPositiveButton("应用", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                runCatching {
                    val startDate = LocalDate.parse(from.text())
                    val endDate = LocalDate.parse(to.text())
                    if (endDate < startDate) throw IllegalArgumentException("结束日期不能早于开始日期")
                    val startWindow = if (useWindow.isChecked) LocalTime.parse(windowStart.text()) else null
                    val endWindow = if (useWindow.isChecked) LocalTime.parse(windowEnd.text()) else null
                    if (startWindow != null && endWindow != null && endWindow <= startWindow) throw IllegalArgumentException("时间段结束必须晚于开始")
                    val offsetMinutes = if (actionSpinner.selectedItemPosition == 0) offset.text().toIntOrNull()
                        ?: throw IllegalArgumentException("请输入移动分钟数") else 0
                    BatchScheduleInput(startDate, endDate, startWindow, endWindow, offsetMinutes)
                }.onSuccess { input ->
                    uiScope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                val selectedCourseId = courseSpinner.selectedItemPosition.takeIf { it > 0 }
                                    ?.let { timetable.allCourses[it - 1].id }
                                var changed = 0
                                timetable.allCourses
                                    .filter { selectedCourseId == null || it.id == selectedCourseId }
                                    .forEach { course ->
                                        course.timeSlots.forEach slotLoop@ { slot ->
                                            if (!ScheduleBatchOperations.overlapsWindow(slot, input.windowStart, input.windowEnd)) return@slotLoop
                                            val updated = ScheduleBatchOperations.applyRange(
                                                slot = slot,
                                                startDate = input.startDate,
                                                endDate = input.endDate,
                                                action = when (actionSpinner.selectedItemPosition) {
                                                    0 -> com.hufeng943.timetable.shared.model.OpenEndedBatchAction.SHIFT
                                                    1 -> com.hufeng943.timetable.shared.model.OpenEndedBatchAction.CANCEL
                                                    else -> com.hufeng943.timetable.shared.model.OpenEndedBatchAction.RESTORE
                                                },
                                                offsetMinutes = input.offsetMinutes,
                                            )
                                            if (updated != slot) {
                                                repository.upsertTimeSlot(updated, course.id)
                                                changed++
                                            }
                                        }
                                    }
                                changed
                            }
                        }.onSuccess { changed ->
                            dialog.dismiss()
                            toast("已更新 $changed 个课时规则")
                        }.onFailure { toast(it.message ?: "批量调整失败") }
                    }
                }.onFailure { toast(it.message ?: "参数错误") }
            }
        }
        dialog.show()
    }

    private data class BatchScheduleInput(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val windowStart: LocalTime?,
        val windowEnd: LocalTime?,
        val offsetMinutes: Int,
    )

    private fun showAcademicEventManagerDialog(timetable: Timetable) {
        val ordered = timetable.events.sortedWith(
            compareBy<AcademicEvent> { it.completed }
                .thenBy { it.date }
                .thenBy { it.time }
                .thenBy { it.title },
        )
        val labels = buildList {
            add("＋ 添加作业 / 考试")
            ordered.forEach { event ->
                val status = if (event.completed) "✓ " else ""
                val title = event.title.ifBlank { "未命名待办" }.let { if (it.length > 24) it.take(23) + "…" else it }
                add("$status${event.date} · ${eventTypeLabel(event.type)} · $title")
            }
        }
        AlertDialog.Builder(this)
            .setTitle("学业待办 · ${timetable.events.count { !it.completed }} 未完成")
            .setItems(labels.toTypedArray()) { _, which ->
                if (which == 0) {
                    showAcademicEventDialog(timetable, null)
                } else {
                    ordered.getOrNull(which - 1)?.let { showAcademicEventActionsDialog(timetable, it) }
                }
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showAcademicEventActionsDialog(timetable: Timetable, event: AcademicEvent) {
        val completeLabel = if (event.completed) "标记为未完成" else "标记为完成"
        val options = arrayOf("编辑", completeLabel, "删除")
        AlertDialog.Builder(this)
            .setTitle(event.title.ifBlank { "学业待办" })
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAcademicEventDialog(timetable, event)
                    1 -> uiScope.launch {
                        withContext(Dispatchers.IO) {
                            repository.upsertAcademicEvent(event.copy(completed = !event.completed), timetable.timetableId)
                        }
                        toast(if (event.completed) "已恢复为未完成" else "已完成")
                    }
                    2 -> AlertDialog.Builder(this)
                        .setTitle("删除待办")
                        .setMessage("确定删除“${event.title}”吗？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除") { _, _ ->
                            uiScope.launch {
                                withContext(Dispatchers.IO) { repository.deleteAcademicEvent(event.id) }
                                toast("待办已删除")
                            }
                        }.show()
                }
            }.show()
    }

    private fun showAcademicEventDialog(timetable: Timetable, existing: AcademicEvent?) {
        val title = inputField("标题", "例如：高数作业第 5 章").apply { editText?.setText(existing?.title.orEmpty()) }
        val typeSpinner = spinner(listOf("作业", "考试", "实验", "其他"))
        typeSpinner.setSelection(existing?.type?.ordinal ?: 0)
        val courseName = autocompleteField(
            "关联课程（可留空）",
            "例如：高等数学",
            timetable.allCourses.map { it.name }.filter { it.isNotBlank() }.distinct(),
        ).apply { editText?.setText(existing?.courseName.orEmpty()) }
        val date = inputField("日期", "YYYY-MM-DD").apply {
            editText?.setText(existing?.date?.toString() ?: todayString())
            editText?.isFocusable = false
            editText?.setOnClickListener { showDatePicker(editText ?: return@setOnClickListener) }
        }
        val time = inputField("时间（可留空）", "例如：14:00；长按清除").apply {
            editText?.setText(existing?.time?.toString().orEmpty())
            editText?.isFocusable = false
            editText?.setOnClickListener { showTimePicker(editText ?: return@setOnClickListener) }
            editText?.setOnLongClickListener {
                editText?.setText("")
                true
            }
        }
        val timeHint = TextView(this).apply {
            text = "未设置时间时，提醒按当天 09:00 计算"
            textSize = 12f
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 0, 0, dp(4))
        }
        val location = autocompleteField(
            "地点（可留空）",
            "例如：A301",
            locationSuggestions(),
        ).apply { editText?.setText(existing?.location.orEmpty()) }
        val note = inputField("备注（可留空）", "最多 200 字").apply { editText?.setText(existing?.note.orEmpty()) }
        val reminderValues = listOf<Int?>(null, 0, 30, 60, 1440)
        val reminderSpinner = spinner(listOf("不提醒", "准时提醒", "提前 30 分钟", "提前 1 小时", "提前 1 天"))
        reminderSpinner.setSelection(reminderValues.indexOf(existing?.reminderMinutesBefore).takeIf { it >= 0 } ?: 0)

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "添加作业 / 考试" else "编辑作业 / 考试")
            .setView(
                dialogColumn(
                    title,
                    labeled("类型", typeSpinner),
                    courseName,
                    date,
                    time,
                    timeHint,
                    location,
                    note,
                    labeled("提醒", reminderSpinner),
                )
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                runCatching {
                    val titleText = title.text().ifBlank { throw IllegalArgumentException("请输入标题") }
                    require(titleText.length <= 80) { "标题最多 80 个字符" }
                    require(note.text().length <= 200) { "备注最多 200 个字符" }
                    val parsedDate = LocalDate.parse(date.text())
                    val parsedTime = time.text().takeIf { it.isNotBlank() }?.let(LocalTime::parse)
                    AcademicEvent(
                        id = existing?.id ?: 0,
                        title = titleText,
                        type = AcademicEventType.entries[typeSpinner.selectedItemPosition],
                        date = parsedDate,
                        time = parsedTime,
                        courseName = courseName.text().takeIf { it.isNotBlank() }?.take(80),
                        location = location.text().takeIf { it.isNotBlank() }?.take(80),
                        note = note.text().takeIf { it.isNotBlank() },
                        reminderMinutesBefore = reminderValues[reminderSpinner.selectedItemPosition],
                        completed = existing?.completed ?: false,
                    )
                }.onSuccess { event ->
                    uiScope.launch {
                        withContext(Dispatchers.IO) { repository.upsertAcademicEvent(event, timetable.timetableId) }
                        dialog.dismiss()
                        if (event.reminderMinutesBefore != null) {
                            if (Build.VERSION.SDK_INT >= 33) requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            requestExactAlarmAccessIfNeeded()
                        }
                        toast(if (existing == null) "待办已添加" else "待办已更新")
                    }
                }.onFailure { toast(it.message ?: "保存失败") }
            }
        }
        dialog.show()
    }

    private fun showCourseActionsDialog(timetable: Timetable, course: Course) {
        val options = arrayOf("编辑课程信息", "编辑上课时间", "复制课程", "删除课程")
        AlertDialog.Builder(this)
            .setTitle(course.name.ifBlank { "课程" })
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditCourseDialog(timetable, course)
                    1 -> showEditTimeSlotDialog(course)
                    2 -> duplicateCourse(timetable, course)
                    3 -> confirmDeleteCourse(course)
                }
            }.show()
    }

    private fun showEditCourseDialog(timetable: Timetable, course: Course) {
        val name = autocompleteField("课程名称", "课程名称", courseSuggestions()).apply { editText?.setText(course.name) }
        val location = autocompleteField("地点（可留空）", "地点", locationSuggestions()).apply { editText?.setText(course.location.orEmpty()) }
        val teacher = autocompleteField("教师（可留空）", "教师", teacherSuggestions()).apply { editText?.setText(course.teacher.orEmpty()) }
        AlertDialog.Builder(this)
            .setTitle("编辑课程")
            .setView(dialogColumn(name, location, teacher))
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val newName = name.text()
                if (newName.isBlank()) {
                    toast("课程名称不能为空")
                    return@setPositiveButton
                }
                uiScope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            repository.upsertCourse(
                                course.copy(
                                    name = newName,
                                    location = location.text().takeIf { it.isNotBlank() },
                                    teacher = teacher.text().takeIf { it.isNotBlank() },
                                ),
                                timetable.timetableId,
                            )
                        }
                    }.onSuccess { toast("课程信息已更新") }
                        .onFailure { toast(it.message ?: "更新失败") }
                }
            }.show()
    }

    private fun showEditTimeSlotDialog(course: Course) {
        if (course.timeSlots.isEmpty()) {
            toast("该课程没有上课时间")
            return
        }
        val slotSpinner = spinner(course.timeSlots.map { slot ->
            "${slot.dayOfWeek?.let(::dayLabel) ?: "未定"} ${slot.startTime ?: "--:--"}-${slot.endTime ?: "--:--"}"
        })
        val daySpinner = spinner(listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日"))
        val recurrenceSpinner = spinner(listOf("每周", "单周", "双周"))
        val start = inputField("开始时间", "08:00")
        val end = inputField("结束时间", "09:40")
        start.editText?.isFocusable = false
        end.editText?.isFocusable = false
        start.editText?.setOnClickListener { showTimePicker(start.editText ?: return@setOnClickListener) }
        end.editText?.setOnClickListener { showTimePicker(end.editText ?: return@setOnClickListener) }
        fun loadSlot(position: Int) {
            val slot = course.timeSlots[position]
            daySpinner.setSelection(slot.dayOfWeek?.ordinal ?: 0)
            recurrenceSpinner.setSelection(slot.recurrence.ordinal)
            start.editText?.setText(slot.startTime?.toString() ?: "08:00")
            end.editText?.setText(slot.endTime?.toString() ?: "09:40")
        }
        loadSlot(0)
        slotSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = loadSlot(position)
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("编辑上课时间")
            .setView(dialogColumn(labeled("课时", slotSpinner), labeled("星期", daySpinner), start, end, labeled("重复", recurrenceSpinner)))
            .setNegativeButton("取消", null)
            .setNeutralButton("删除此课时", null)
            .setPositiveButton("保存", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                runCatching {
                    val slot = course.timeSlots[slotSpinner.selectedItemPosition]
                    val startTime = LocalTime.parse(start.text())
                    val endTime = LocalTime.parse(end.text())
                    if (endTime <= startTime) throw IllegalArgumentException("结束时间必须晚于开始时间")
                    slot.copy(
                        dayOfWeek = DayOfWeek.entries[daySpinner.selectedItemPosition],
                        startTime = startTime,
                        endTime = endTime,
                        recurrence = WeekPattern.entries[recurrenceSpinner.selectedItemPosition],
                    )
                }.onSuccess { updated ->
                    uiScope.launch {
                        withContext(Dispatchers.IO) { repository.upsertTimeSlot(updated, course.id) }
                        dialog.dismiss()
                        toast("上课时间已更新")
                    }
                }.onFailure { toast(it.message ?: "更新失败") }
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val slot = course.timeSlots[slotSpinner.selectedItemPosition]
                uiScope.launch {
                    withContext(Dispatchers.IO) { repository.deleteTimeSlot(slot.id) }
                    dialog.dismiss()
                    toast("课时已删除")
                }
            }
        }
        dialog.show()
    }

    private fun duplicateCourse(timetable: Timetable, source: Course) {
        uiScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val newCourseId = repository.upsertCourse(
                        source.copy(id = 0, name = source.name + " 副本", timeSlots = emptyList()),
                        timetable.timetableId,
                    )
                    source.timeSlots.forEach { repository.upsertTimeSlot(it.copy(id = 0), newCourseId) }
                }
            }.onSuccess { toast("课程已复制") }
                .onFailure { toast(it.message ?: "复制失败") }
        }
    }

    private fun confirmDeleteCourse(course: Course) {
        AlertDialog.Builder(this)
            .setTitle("删除课程")
            .setMessage("确定删除“${course.name}”及其全部课时吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                uiScope.launch {
                    withContext(Dispatchers.IO) { repository.deleteCourse(course.id) }
                    toast("课程已删除")
                }
            }.show()
    }

    private fun duplicateTimetable(source: Timetable) {
        uiScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val newTableId = repository.upsertTimetable(
                        source.copy(
                            timetableId = 0,
                            semesterName = source.semesterName + " 副本",
                            createdAt = Clock.System.now(),
                            allCourses = emptyList(),
                            events = emptyList(),
                        )
                    )
                    source.allCourses.forEach { course ->
                        val newCourseId = repository.upsertCourse(course.copy(id = 0, timeSlots = emptyList()), newTableId)
                        course.timeSlots.forEach { slot -> repository.upsertTimeSlot(slot.copy(id = 0), newCourseId) }
                    }
                    source.events.forEach { event -> repository.upsertAcademicEvent(event.copy(id = 0), newTableId) }
                }
            }.onSuccess { toast("课表已复制") }
                .onFailure { toast(it.message ?: "复制失败") }
        }
    }

    private fun requestCalendarSync(timetable: Timetable) {
        if (SystemCalendarSync.hasPermission(this)) {
            syncTimetableToCalendar(timetable)
        } else {
            pendingCalendarTimetable = timetable
            pendingCalendarAction = CalendarAction.SYNC
            requestCalendarPermissions.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
        }
    }

    private fun requestCalendarClear(timetable: Timetable) {
        if (SystemCalendarSync.hasPermission(this)) {
            clearTimetableFromCalendar(timetable)
        } else {
            pendingCalendarTimetable = timetable
            pendingCalendarAction = CalendarAction.CLEAR
            requestCalendarPermissions.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
        }
    }

    private fun clearTimetableFromCalendar(timetable: Timetable) {
        uiScope.launch {
            runCatching { withContext(Dispatchers.IO) { SystemCalendarSync.clear(this@MainActivity, timetable) } }
                .onSuccess { deleted ->
                    if (deleted > 0) toast("已清除 $deleted 条 Timetable 日历日程")
                    else toast("系统日历中没有该课表添加的日程")
                }
                .onFailure { toast(it.message ?: "清除系统日历失败") }
        }
    }

    private fun syncTimetableToCalendar(timetable: Timetable) {
        uiScope.launch {
            runCatching { withContext(Dispatchers.IO) { SystemCalendarSync.sync(this@MainActivity, timetable) } }
                .onSuccess { toast("已同步 ${it.inserted} 条日程到 ${it.calendarName}") }
                .onFailure { toast(it.message ?: "系统日历同步失败") }
        }
    }

    private fun showScheduleOverrideDialog(
        timetable: Timetable,
        preselectedCourseId: Long? = null,
        preselectedType: ScheduleOverrideType? = null,
    ) {
        val choices = timetable.allCourses.flatMap { course -> course.timeSlots.map { course to it } }
        if (choices.isEmpty()) {
            toast("请先添加课程和上课时间")
            return
        }
        val slotSpinner = spinner(choices.map { (course, slot) ->
            "${course.name} · ${slot.dayOfWeek?.let(::dayLabel) ?: "未定"} ${slot.startTime ?: "--:--"}-${slot.endTime ?: "--:--"}"
        })
        val typeValues = listOf("停课", "调课/修改", "临时加课", "恢复正常")
        val typeSpinner = spinner(typeValues).apply {
            setSelection(
                when (preselectedType) {
                    ScheduleOverrideType.CANCELLED -> 0
                    ScheduleOverrideType.MODIFIED -> 1
                    ScheduleOverrideType.EXTRA -> 2
                    null -> 0
                }
            )
        }
        val date = inputField("日期", "YYYY-MM-DD").apply { editText?.setText(todayString()) }
        date.editText?.isFocusable = false
        date.editText?.setOnClickListener { showDatePicker(date.editText ?: return@setOnClickListener) }
        val start = inputField("新开始时间（停课可忽略）", "08:00")
        val end = inputField("新结束时间（停课可忽略）", "09:40")
        start.editText?.isFocusable = false
        end.editText?.isFocusable = false
        fun fillTimes(position: Int) {
            val slot = choices[position].second
            start.editText?.setText(slot.startTime?.toString() ?: "08:00")
            end.editText?.setText(slot.endTime?.toString() ?: "09:40")
        }
        val initialIndex = choices.indexOfFirst { (course, _) -> course.id == preselectedCourseId }.takeIf { it >= 0 } ?: 0
        slotSpinner.setSelection(initialIndex)
        fillTimes(initialIndex)
        slotSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = fillTimes(position)
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        start.editText?.setOnClickListener { showTimePicker(start.editText ?: return@setOnClickListener) }
        end.editText?.setOnClickListener { showTimePicker(end.editText ?: return@setOnClickListener) }
        val location = inputField("临时地点（可留空）", "例如：A303")

        val dialog = AlertDialog.Builder(this)
            .setTitle("日期例外")
            .setView(dialogColumn(labeled("课程", slotSpinner), labeled("类型", typeSpinner), date, start, end, location))
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                runCatching {
                    val (course, slot) = choices[slotSpinner.selectedItemPosition]
                    val targetDate = LocalDate.parse(date.text())
                    if (typeSpinner.selectedItemPosition == 3) {
                        course to slot.copy(overrides = slot.overrides.filterNot { it.date == targetDate })
                    } else {
                        val type = when (typeSpinner.selectedItemPosition) {
                            0 -> ScheduleOverrideType.CANCELLED
                            1 -> ScheduleOverrideType.MODIFIED
                            else -> ScheduleOverrideType.EXTRA
                        }
                        val override = if (type == ScheduleOverrideType.CANCELLED) {
                            ScheduleOverride(targetDate, type)
                        } else {
                            val startTime = LocalTime.parse(start.text())
                            val endTime = LocalTime.parse(end.text())
                            if (endTime <= startTime) throw IllegalArgumentException("结束时间必须晚于开始时间")
                            ScheduleOverride(
                                date = targetDate,
                                type = type,
                                startTime = startTime,
                                endTime = endTime,
                                location = location.text().takeIf { it.isNotBlank() },
                            )
                        }
                        course to slot.copy(overrides = slot.overrides.filterNot { it.date == targetDate } + override)
                    }
                }.onSuccess { (course, updatedSlot) ->
                    uiScope.launch {
                        withContext(Dispatchers.IO) { repository.upsertTimeSlot(updatedSlot, course.id) }
                        dialog.dismiss()
                        toast("日期例外已保存并加入同步队列")
                    }
                }.onFailure { toast(it.message ?: "保存失败") }
            }
        }
        dialog.show()
    }

    private fun showReminderSettingsDialog() {
        val enabled = CheckBox(this).apply {
            text = "启用课程提醒"
            isChecked = ReminderSettings.enabled(this@MainActivity)
        }
        val values = listOf(5, 10, 15, 30)
        val offsetSpinner = spinner(values.map { "提前 $it 分钟" })
        val current = ReminderSettings.offsetMinutes(this)
        offsetSpinner.setSelection(values.indexOf(current).takeIf { it >= 0 } ?: 1)
        AlertDialog.Builder(this)
            .setTitle("课程提醒")
            .setView(dialogColumn(enabled, labeled("提醒时间", offsetSpinner)))
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val offset = values[offsetSpinner.selectedItemPosition]
                ReminderSettings.save(this, enabled.isChecked, offset)
                CourseReminderScheduler.schedule(this, currentTimetables)
                if (enabled.isChecked && Build.VERSION.SDK_INT >= 33) {
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (enabled.isChecked) requestExactAlarmAccessIfNeeded()
                toast(if (enabled.isChecked) "已启用：提前 $offset 分钟提醒" else "课程提醒已关闭")
            }.show()
    }

    private fun requestExactAlarmAccessIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = getSystemService(AlarmManager::class.java)
        if (alarmManager.canScheduleExactAlarms()) return
        AlertDialog.Builder(this)
            .setTitle("允许精确课程提醒")
            .setMessage("允许“闹钟和提醒”权限后，课前和开课通知可以更准时；不授权也能继续使用近似提醒。")
            .setNegativeButton("使用近似提醒", null)
            .setPositiveButton("去设置") { _, _ ->
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                }.onFailure { toast("无法打开闹钟权限设置") }
            }
            .show()
    }

    private fun confirmDelete(timetable: Timetable) {
        AlertDialog.Builder(this)
            .setTitle("删除课表")
            .setMessage("确定删除“${timetable.semesterName}”及其全部课程吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                uiScope.launch {
                    withContext(Dispatchers.IO) { repository.deleteTimetable(timetable.timetableId) }
                    toast("已删除")
                }
            }.show()
    }

    private fun inputField(hint: String, placeholder: String): TextInputLayout =
        TextInputLayout(this).apply {
            this.hint = hint
            placeholderText = placeholder
            setPadding(0, dp(4), 0, dp(4))
            addView(TextInputEditText(context))
        }

    private fun autocompleteField(hint: String, placeholder: String, suggestions: List<String>): TextInputLayout =
        TextInputLayout(this).apply {
            this.hint = hint
            placeholderText = placeholder
            setPadding(0, dp(4), 0, dp(4))
            addView(MaterialAutoCompleteTextView(context).apply {
                threshold = 1
                setAdapter(ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, suggestions.distinct().take(50)))
            })
        }

    private fun courseSuggestions(): List<String> = currentTimetables
        .flatMap { it.allCourses }
        .map { it.name.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    private fun locationSuggestions(): List<String> = currentTimetables
        .flatMap { table -> table.allCourses.mapNotNull { it.location } + table.events.mapNotNull { it.location } }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    private fun teacherSuggestions(): List<String> = currentTimetables
        .flatMap { it.allCourses }
        .mapNotNull { it.teacher?.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    private fun eventTypeLabel(type: AcademicEventType): String = when (type) {
        AcademicEventType.ASSIGNMENT -> "作业"
        AcademicEventType.EXAM -> "考试"
        AcademicEventType.LAB -> "实验"
        AcademicEventType.OTHER -> "待办"
    }

    private fun formatMinutes(minutes: Int): String = when {
        minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60} 小时"
        minutes >= 60 -> "${minutes / 60}小时${minutes % 60}分"
        else -> "${minutes} 分钟"
    }

    private fun TextInputLayout.text(): String = editText?.text?.toString()?.trim().orEmpty()

    private fun dialogColumn(vararg views: View): ScrollView = ScrollView(this).apply {
        isFillViewport = true
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), dp(8))
            views.forEach { view -> addView(view) }
        })
    }

    private fun labeled(label: String, view: View): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(4), 0, dp(4))
        addView(TextView(context).apply { text = label })
        addView(view)
    }

    private fun spinner(values: List<String>): Spinner = Spinner(this).apply {
        adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, values)
    }

    private fun showDatePicker(target: EditText) {
        val initial = runCatching { LocalDate.parse(target.text.toString()) }.getOrNull()
        val now = java.time.LocalDate.now()
        DatePickerDialog(
            this,
            { _, year, month, day -> target.setText("%04d-%02d-%02d".format(year, month + 1, day)) },
            initial?.year ?: now.year,
            (initial?.monthNumber ?: now.monthValue) - 1,
            initial?.day ?: now.dayOfMonth
        ).show()
    }

    private fun showTimePicker(target: EditText) {
        val initial = runCatching { LocalTime.parse(target.text.toString()) }.getOrNull()
        TimePickerDialog(
            this,
            { _, hour, minute -> target.setText("%02d:%02d".format(hour, minute)) },
            initial?.hour ?: 8,
            initial?.minute ?: 0,
            true
        ).show()
    }

    private fun dayLabel(day: DayOfWeek): String =
        listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[day.ordinal]

    private fun todayString(): String = java.time.LocalDate.now().toString()

    private fun resolveSurfaceColor(): Int {
        val value = android.util.TypedValue()
        return if (theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainer, value, true)) {
            value.data
        } else Color.WHITE
    }

    private fun resolvePrimaryColor(): Int {
        val value = android.util.TypedValue()
        return if (theme.resolveAttribute(android.R.attr.colorAccent, value, true)) {
            value.data
        } else 0xFF6750A4.toInt()
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized && currentTimetables.isNotEmpty()) {
            CourseReminderScheduler.schedule(this, currentTimetables)
        }
    }

    override fun onDestroy() {
        uiScope.cancel()
        super.onDestroy()
    }
}
