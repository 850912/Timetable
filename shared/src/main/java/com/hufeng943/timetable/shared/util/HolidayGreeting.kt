package com.hufeng943.timetable.shared.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

data class HolidayGreeting(val name: String, val message: String)

fun holidayGreeting(date: LocalDate): HolidayGreeting? {
    val fixed = when (date.monthNumber to date.dayOfMonth) {
        1 to 1 -> "元旦" to "新年快乐，愿每一天都有新的收获！"
        2 to 14 -> "情人节" to "情人节快乐，愿喜欢的人和事都在身边！"
        3 to 8 -> "妇女节" to "妇女节快乐，愿你一直闪闪发光！"
        3 to 12 -> "植树节" to "植树节快乐，愿我们一起守护绿色地球！"
        3 to 15 -> "消费者权益日" to "愿每一次消费都安心、透明、有保障！"
        4 to 1 -> "愚人节" to "愚人节快乐，保持幽默，也别忘了认真生活！"
        4 to 22 -> "世界地球日" to "地球日快乐，从身边的小事开始爱护地球！"
        5 to 1 -> "劳动节" to "劳动节快乐，愿忙碌之后都有好好休息！"
        5 to 4 -> "青年节" to "青年节快乐，愿青春热烈，梦想常在！"
        5 to 12 -> "护士节" to "护士节快乐，感谢每一份守护与温柔！"
        6 to 1 -> "儿童节" to "儿童节快乐，愿你永远保有好奇和快乐！"
        6 to 5 -> "世界环境日" to "环境日快乐，让绿色成为每天的选择！"
        7 to 1 -> "建党节" to "建党节快乐！"
        8 to 1 -> "建军节" to "建军节快乐，致敬守护和平的力量！"
        9 to 10 -> "教师节" to "教师节快乐，感谢每一份耐心与启发！"
        10 to 1 -> "国庆节" to "国庆节快乐，愿山河无恙，万事顺意！"
        10 to 24 -> "联合国日" to "愿世界和平，合作与理解常在！"
        11 to 11 -> "光棍节" to "今天也要好好爱自己，快乐加倍！"
        12 to 24 -> "平安夜" to "平安夜快乐，愿平安与温暖相伴！"
        12 to 25 -> "圣诞节" to "圣诞快乐，愿今天有惊喜，也有温暖！"
        else -> null
    }
    if (fixed != null) return HolidayGreeting(fixed.first, fixed.second)

    lunarHoliday(date)?.let { return it }
    if (date == easterSunday(date.year)) return HolidayGreeting("复活节", "复活节快乐，愿新生与希望常伴！")
    if (date == fourthThursdayOfNovember(date.year)) return HolidayGreeting("感恩节", "感恩节快乐，愿心怀感恩，温暖常在！")
    return null
}

private fun lunarHoliday(date: LocalDate): HolidayGreeting? = lunarDates[date.year]?.entries
    ?.firstOrNull { it.value == (date.monthNumber to date.dayOfMonth) }
    ?.key?.let { name ->
        val message = when (name) {
            "春节" -> "春节快乐，愿新春大吉，阖家幸福！"
            "元宵节" -> "元宵节快乐，愿团圆常在，灯火可亲！"
            "端午节" -> "端午安康，愿顺遂如意，平安常伴！"
            "七夕" -> "七夕快乐，愿所爱皆如愿！"
            "中秋节" -> "中秋节快乐，愿月圆人团圆！"
            "重阳节" -> "重阳安康，愿长辈健康，岁岁常欢！"
            else -> "节日快乐，愿今天有温暖和好心情！"
        }
        HolidayGreeting(name, message)
    }

private fun fourthThursdayOfNovember(year: Int): LocalDate {
    var date = LocalDate(year, 11, 22)
    while (date.dayOfWeek.isoDayNumber != 4) date = date.plus(DatePeriod(days = 1))
    return date
}

private fun easterSunday(year: Int): LocalDate {
    val a = year % 19; val b = year / 100; val c = year % 100
    val d = b / 4; val e = b % 4; val f = (b + 8) / 25; val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30; val i = c / 4; val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7; val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31; val day = (h + l - 7 * m + 114) % 31 + 1
    return LocalDate(year, month, day)
}

// Common lunar festivals for the current release window. Keep this table explicit so
// devices do not need a timezone-sensitive or platform-specific lunar library.
private val lunarDates = mapOf(
    2024 to mapOf("春节" to (2 to 10), "元宵节" to (2 to 24), "端午节" to (6 to 10), "七夕" to (8 to 10), "中秋节" to (9 to 17), "重阳节" to (10 to 11)),
    2025 to mapOf("春节" to (1 to 29), "元宵节" to (2 to 12), "端午节" to (5 to 31), "七夕" to (8 to 29), "中秋节" to (10 to 6), "重阳节" to (10 to 29)),
    2026 to mapOf("春节" to (2 to 17), "元宵节" to (3 to 3), "端午节" to (6 to 19), "七夕" to (8 to 19), "中秋节" to (9 to 25), "重阳节" to (10 to 18)),
    2027 to mapOf("春节" to (2 to 6), "元宵节" to (2 to 20), "端午节" to (6 to 9), "七夕" to (8 to 8), "中秋节" to (9 to 15), "重阳节" to (10 to 15)),
    2028 to mapOf("春节" to (1 to 26), "元宵节" to (2 to 9), "端午节" to (5 to 27), "七夕" to (8 to 2), "中秋节" to (10 to 3), "重阳节" to (10 to 4)),
    2029 to mapOf("春节" to (2 to 13), "元宵节" to (2 to 27), "端午节" to (6 to 16), "七夕" to (8 to 20), "中秋节" to (9 to 22), "重阳节" to (10 to 11)),
    2030 to mapOf("春节" to (2 to 3), "元宵节" to (2 to 17), "端午节" to (6 to 5), "七夕" to (8 to 9), "中秋节" to (9 to 12), "重阳节" to (10 to 2)),
)
