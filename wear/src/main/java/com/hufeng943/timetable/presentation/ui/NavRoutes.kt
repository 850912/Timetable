package com.hufeng943.timetable.presentation.ui

object NavArgs {
    const val TIME_SLOT_ID = "timeSlotId"
    const val TABLE_ID = "tableId"
    const val COURSE_ID = "courseId"
}

/**
 * App-level routes for the Wear app.
 *
 * All visible destinations live in one Navigation Compose graph. Forward transitions are kept
 * non-spatial to avoid the round-screen old-page/left-edge artifact. On API 36+ Navigation
 * Compose handles platform predictive back; older Wear versions use a BasicSwipeToDismissBox
 * around the graph. Nested graphs below are state/lifecycle scopes, not nested NavHosts.
 */
object NavRoutes {
    const val MAIN = "main"

    //----------------------------------
    const val COURSE_DETAIL = "course_detail/{${NavArgs.TIME_SLOT_ID}}"
    fun courseDetail(timeSlotId: Long) = "course_detail/$timeSlotId"

    //----------------------------------
    const val LIST_TIMETABLE = "list_timetable"

    // Batch schedule tools graph.
    const val SCHEDULE_TOOLS = "schedule_tools"
    const val SCHEDULE_TOOLS_MAIN = "schedule_tools/main"
    const val SCHEDULE_TOOLS_START = "schedule_tools/start"
    const val SCHEDULE_TOOLS_END = "schedule_tools/end"
    const val SCHEDULE_TOOLS_WINDOW_START = "schedule_tools/window_start"
    const val SCHEDULE_TOOLS_WINDOW_END = "schedule_tools/window_end"
    const val SCHEDULE_TOOLS_DAYS = "schedule_tools/days"
    const val SCHEDULE_TOOLS_OFFSET = "schedule_tools/offset"

    // Timetable editor graph.
    const val EDIT_TIMETABLE = "edit_timetable/{${NavArgs.TABLE_ID}}"
    fun editTimetable(timetableId: Long? = null) = "edit_timetable/${timetableId ?: -1L}"
    const val EDIT_TIMETABLE_MAIN = "edit_timetable/main"
    const val EDIT_TIMETABLE_NAME = "edit_timetable/name"
    const val EDIT_TIMETABLE_START_DATE = "edit_timetable/start_date"
    const val EDIT_TIMETABLE_END_DATE = "edit_timetable/end_date"
    const val EDIT_TIMETABLE_COLOR = "edit_timetable/color"
    const val EDIT_TIMETABLE_DELETE_CONFIRM = "edit_timetable/delete_confirm"

    //----------------------------------
    const val LIST_COURSE = "list_course/{${NavArgs.TABLE_ID}}"
    fun listCourse(timetableId: Long) = "list_course/$timetableId"

    const val EDIT_COURSE = "edit_course/{${NavArgs.TABLE_ID}}/{${NavArgs.COURSE_ID}}"
    fun editCourse(timetableId: Long, courseId: Long? = null) =
        "edit_course/$timetableId/${courseId ?: -1L}"

    const val EDIT_COURSE_MAIN = "edit_course/main"
    const val EDIT_COURSE_NAME = "edit_course/{${NavArgs.TABLE_ID}}/{${NavArgs.COURSE_ID}}/name"
    const val EDIT_COURSE_LOCATION = "edit_course/{${NavArgs.TABLE_ID}}/{${NavArgs.COURSE_ID}}/location"
    const val EDIT_COURSE_TEACHER = "edit_course/{${NavArgs.TABLE_ID}}/{${NavArgs.COURSE_ID}}/teacher"
    const val EDIT_COURSE_COLOR = "edit_course/{${NavArgs.TABLE_ID}}/{${NavArgs.COURSE_ID}}/color"
    const val EDIT_COURSE_DELETE_CONFIRM = "edit_course/{${NavArgs.TABLE_ID}}/{${NavArgs.COURSE_ID}}/delete_confirm"

    fun editCourseName(timetableId: Long, courseId: Long) =
        "edit_course/$timetableId/$courseId/name"
    fun editCourseLocation(timetableId: Long, courseId: Long) =
        "edit_course/$timetableId/$courseId/location"
    fun editCourseTeacher(timetableId: Long, courseId: Long) =
        "edit_course/$timetableId/$courseId/teacher"
    fun editCourseColor(timetableId: Long, courseId: Long) =
        "edit_course/$timetableId/$courseId/color"
    fun editCourseDeleteConfirm(timetableId: Long, courseId: Long) =
        "edit_course/$timetableId/$courseId/delete_confirm"

    //----------------------------------
    const val LIST_TIMESLOT = "list_timeslot/{${NavArgs.COURSE_ID}}"
    fun listTimeSlot(courseId: Long) = "list_timeslot/$courseId"

    // Time-slot editor graph.
    const val EDIT_TIMESLOT = "edit_timeslot/{${NavArgs.COURSE_ID}}/{${NavArgs.TIME_SLOT_ID}}"
    fun editTimeSlot(courseId: Long, timeSlotId: Long? = null) =
        "edit_timeslot/$courseId/${timeSlotId ?: -1L}"
    const val EDIT_TIMESLOT_MAIN = "edit_timeslot/main"
    const val EDIT_TIMESLOT_START_TIME = "edit_timeslot/start_time"
    const val EDIT_TIMESLOT_END_TIME = "edit_timeslot/end_time"
    const val EDIT_TIMESLOT_DATES = "edit_timeslot/dates"
    const val EDIT_TIMESLOT_WEEK_DAY = "edit_timeslot/week_day"
    const val EDIT_TIMESLOT_RECURRENCE = "edit_timeslot/recurrence"
    const val EDIT_TIMESLOT_REMARK = "edit_timeslot/remark"
    const val EDIT_TIMESLOT_DELETE_CONFIRM = "edit_timeslot/delete_confirm"

    //----------------------------------
    const val MORE_ABOUT = "more/about"
    const val MORE_ABOUT_LIBRARIES = "more/about/libraries"
    const val MORE_ABOUT_DEVELOPER = "more/about/developer"

    // Settings graph.
    const val MORE_SETTINGS = "more/settings"
    const val MORE_SETTINGS_MAIN = "more/settings/main"
    const val MORE_SETTINGS_UI = "more/settings/ui"
    const val MORE_SETTINGS_LANGUAGE = "more/settings/language"
    const val MORE_SETTINGS_TIME_FORMAT = "more/settings/time_format"
    const val MORE_SETTINGS_FIRST_DAY = "more/settings/first_day"
    const val MORE_SETTINGS_POWER_SAVE = "more/settings/power_save"
    const val MORE_SETTINGS_EXPORT = "more/settings/export"
    const val MORE_SETTINGS_IMPORT = "more/settings/import"
    const val MORE_SETTINGS_THEME = "more/settings/theme"
    const val MORE_SETTINGS_BACKGROUND = "more/settings/background"
    const val MORE_SETTINGS_LIQUID_GLASS = "more/settings/liquid_glass"
    const val MORE_SETTINGS_GLASS_OPACITY = "more/settings/liquid_glass/opacity"
    const val MORE_SETTINGS_GLASS_CLARITY = "more/settings/liquid_glass/clarity"
    const val MORE_SETTINGS_BACKGROUND_BRIGHTNESS = "more/settings/liquid_glass/background_brightness"
    const val MORE_SETTINGS_BACKGROUND_IMAGE_BLUR = "more/settings/background/image_blur"

    // Day arrangement graph.
    const val MORE_DAY_ARRANGEMENT = "more/day_arrangement"
    const val MORE_DAY_ARRANGEMENT_MAIN = "more/day_arrangement/main"
    const val MORE_DAY_ARRANGEMENT_DATE = "more/day_arrangement/date"
    const val MORE_DAY_ARRANGEMENT_SOURCE = "more/day_arrangement/source"

    // Course adjustment graph.
    const val MORE_COURSE_ADJUSTMENT = "more/course_adjustment"
    const val MORE_COURSE_ADJUSTMENT_MAIN = "more/course_adjustment/main"
    const val MORE_COURSE_ADJUSTMENT_DATE = "more/course_adjustment/date"
    const val MORE_COURSE_ADJUSTMENT_A_COURSES = "more/course_adjustment/a_courses"
    const val MORE_COURSE_ADJUSTMENT_A_SLOTS = "more/course_adjustment/a_slots"
    const val MORE_COURSE_ADJUSTMENT_B_COURSES = "more/course_adjustment/b_courses"
    const val MORE_COURSE_ADJUSTMENT_B_SLOTS = "more/course_adjustment/b_slots"
}
