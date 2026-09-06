package com.hufeng943.timetable

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.importexport.TimetableFileParser
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import com.hufeng943.timetable.transfer.PhoneWearSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Clock

class MainActivity : AppCompatActivity() {
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: TimetableRepository
    private lateinit var timetableContainer: LinearLayout
    private lateinit var emptyText: TextView
    private var currentTimetables: List<Timetable> = emptyList()

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importTimetableFile(uri)
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
        timetableContainer = findViewById(R.id.timetableContainer)
        emptyText = findViewById(R.id.emptyText)

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
            syncToWatch(currentTimetables)
        }

        observeTimetables()
    }

    private fun observeTimetables() {
        uiScope.launch {
            repository.getAllTimetables().collectLatest { timetables ->
                currentTimetables = timetables.sortedByDescending { it.createdAt }
                renderTimetables(currentTimetables)
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
                    cornerRadius = dp(18).toFloat()
                    setColor(resolveSurfaceColor())
                    setStroke(dp(1), 0x22000000)
                }
            }
            card.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }

            card.addView(TextView(this).apply {
                text = timetable.semesterName.ifBlank { "未命名课表" }
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            card.addView(TextView(this).apply {
                val end = timetable.semesterEnd?.toString() ?: "长期"
                text = "${timetable.semesterStart} ～ $end · ${timetable.allCourses.size} 门课程"
                setPadding(0, dp(5), 0, dp(8))
            })

            timetable.allCourses.forEach { course ->
                val slotText = course.timeSlots.joinToString("；") { slot ->
                    val day = slot.dayOfWeek?.let(::dayLabel) ?: "未定"
                    val start = slot.startTime?.toString() ?: "--:--"
                    val end = slot.endTime?.toString() ?: "--:--"
                    "$day $start-$end"
                }.ifBlank { "暂无时间" }
                card.addView(TextView(this).apply {
                    text = "• ${course.name}  $slotText" +
                        listOfNotNull(course.location, course.teacher).takeIf { it.isNotEmpty() }
                            ?.joinToString(prefix = "  （", postfix = "）", separator = " / ").orEmpty()
                    setPadding(0, dp(3), 0, dp(3))
                })
            }

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(8), 0, 0)
            }
            actions.addView(actionButton("添加课程") { showAddCourseDialog(timetable) })
            actions.addView(actionButton("同步") { syncToWatch(listOf(timetable)) })
            actions.addView(actionButton("删除") { confirmDelete(timetable) })
            card.addView(actions)
            timetableContainer.addView(card)
        }
    }

    private fun actionButton(label: String, action: () -> Unit): MaterialButton =
        MaterialButton(this).apply {
            text = label
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(4)
            }
            setOnClickListener { action() }
        }

    private fun showCreateTimetableDialog() {
        val name = inputField("课表名称", "例如：2026 秋季学期")
        val start = inputField("开始日期", "YYYY-MM-DD").apply { editText?.setText(todayString()) }
        val end = inputField("结束日期（可留空）", "YYYY-MM-DD")
        start.editText?.setOnClickListener { showDatePicker(start.editText as EditText) }
        end.editText?.setOnClickListener { showDatePicker(end.editText as EditText) }
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
        val name = inputField("课程名称", "例如：高等数学")
        val location = inputField("地点（可留空）", "例如：教学楼 A101")
        val teacher = inputField("教师（可留空）", "例如：张老师")
        val start = inputField("开始时间", "08:00").apply { editText?.setText("08:00") }
        val end = inputField("结束时间", "09:40").apply { editText?.setText("09:40") }
        start.editText?.isFocusable = false
        end.editText?.isFocusable = false
        start.editText?.setOnClickListener { showTimePicker(start.editText as EditText) }
        end.editText?.setOnClickListener { showTimePicker(end.editText as EditText) }

        val daySpinner = spinner(listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日"))
        val recurrenceSpinner = spinner(listOf("每周", "单周", "双周"))
        val content = dialogColumn(
            name, location, teacher,
            labeled("星期", daySpinner),
            start, end,
            labeled("重复", recurrenceSpinner)
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
                    val slot = TimeSlot(
                        startTime = startTime,
                        endTime = endTime,
                        dayOfWeek = DayOfWeek.entries[daySpinner.selectedItemPosition],
                        recurrence = WeekPattern.entries[recurrenceSpinner.selectedItemPosition]
                    )
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
        start.editText?.setOnClickListener { showDatePicker(start.editText as EditText) }
        end.editText?.setOnClickListener { showDatePicker(end.editText as EditText) }

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
            .map { sameCourseRows ->
                sameCourseRows.first().copy(
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
                }
                timetables.size
            }.onSuccess { toast("成功导入 $it 个课表") }
                .onFailure { toast(it.message ?: "导入失败") }
        }
    }

    private fun syncToWatch(timetables: List<Timetable>) {
        if (timetables.isEmpty()) {
            toast("没有可同步的课表")
            return
        }
        uiScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    PhoneWearSyncManager.send(this@MainActivity, timetables)
                }
            }.onSuccess { device -> toast("已发送到 $device") }
                .onFailure { toast(it.message ?: "同步失败") }
        }
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
            setPadding(0, dp(4), 0, dp(4))
            addView(TextInputEditText(context).apply { this.hint = placeholder })
        }

    private fun TextInputLayout.text(): String = editText?.text?.toString()?.trim().orEmpty()

    private fun dialogColumn(vararg views: View): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(4), dp(20), 0)
        views.forEach { view -> addView(view) }
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        uiScope.cancel()
        super.onDestroy()
    }
}
