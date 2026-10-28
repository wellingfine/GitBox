package com.v0tx.gitbox.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

private val MONTH_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun LocalDate.toDayStartMillis(): Long =
    this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDate.monthKey(): String = this.format(MONTH_KEY_FORMATTER)

fun LocalDate.displayText(): String = this.format(DATE_FORMATTER)

fun Long.displayTime(): String = TIME_FORMATTER.format(this.toLocalDateTime())

fun Long.displayDateTime(): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(this.toLocalDateTime())

/** 一周的第一天按周日计算，与 GitHub 贡献墙保持一致 */
fun LocalDate.startOfWeek(): LocalDate {
    var d = this
    while (d.dayOfWeek.value % 7 != 0) { // SUNDAY.value == 7
        d = d.minusDays(1)
    }
    return d
}

fun LocalDate.weekDayIndex(): Int = this.dayOfWeek.value % 7 // 周日 = 0

fun LocalDate.weekLabel(locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("EEE", locale).format(this)

fun LocalDate.monthLabel(locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("MMM", locale).format(this)

fun LocalDate.weekOfYearValue(): Int =
    this.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
