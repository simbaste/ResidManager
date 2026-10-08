package com.resid.manager.ui.components

import com.resid.manager.getCurrentEpochMillis

/**
 * Returns today's date formatted as an ISO-8601 string ("YYYY-MM-DD").
 */
fun getTodayIsoDate(): String {
    return formatUtcMillisToIsoDate(getCurrentEpochMillis())
}

/**
 * Format UTC epoch milliseconds to ISO-8601 date string ("YYYY-MM-DD").
 */
fun formatUtcMillisToIsoDate(utcMillis: Long): String {
    val totalDays = (utcMillis / 86400000L).toInt()
    val days = totalDays + 719468
    val era = if (days >= 0) days / 146097 else (days - 146096) / 146097
    val doe = days - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = mp + (if (mp < 10) 3 else -9)
    val year = y + (if (m <= 2) 1 else 0)

    val mStr = if (m < 10) "0$m" else "$m"
    val dStr = if (d < 10) "0$d" else "$d"
    return "$year-$mStr-$dStr"
}

/**
 * Parses "YYYY-MM-DD" into UTC epoch milliseconds.
 */
fun parseDateToUtcMillis(isoDate: String): Long? {
    val parts = isoDate.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null

    var days = 0L
    if (year >= 1970) {
        for (y in 1970 until year) {
            days += if (isLeapYear(y)) 366 else 365
        }
    } else {
        for (y in year until 1970) {
            days -= if (isLeapYear(y)) 366 else 365
        }
    }

    val daysInMonths = intArrayOf(
        31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    )

    for (m in 1 until month) {
        days += daysInMonths[m - 1]
    }
    days += (day - 1)

    return days * 86400000L
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}
