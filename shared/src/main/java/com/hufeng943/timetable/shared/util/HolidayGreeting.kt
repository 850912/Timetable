package com.hufeng943.timetable.shared.util

import kotlinx.datetime.LocalDate

data class HolidayGreeting(val name: String, val message: String)

fun holidayGreeting(date: LocalDate): HolidayGreeting? = when (date.monthNumber to date.dayOfMonth) {
    1 to 1 -> HolidayGreeting("元旦", "新年快乐，愿每一天都有新的收获！")
    2 to 14 -> HolidayGreeting("情人节", "情人节快乐，愿喜欢的人和事都在身边！")
    3 to 8 -> HolidayGreeting("妇女节", "妇女节快乐，愿你一直闪闪发光！")
    5 to 1 -> HolidayGreeting("劳动节", "劳动节快乐，愿忙碌之后都有好好休息！")
    6 to 1 -> HolidayGreeting("儿童节", "儿童节快乐，愿你永远保有好奇和快乐！")
    9 to 10 -> HolidayGreeting("教师节", "教师节快乐，感谢每一份耐心与启发！")
    10 to 1 -> HolidayGreeting("国庆节", "国庆节快乐，愿山河无恙，万事顺意！")
    12 to 25 -> HolidayGreeting("圣诞节", "圣诞快乐，愿今天有惊喜，也有温暖！")
    else -> null
}
