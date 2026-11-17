package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private const val MILLIS_PER_DAY = 86_400_000L
    private val spanishLocale = Locale("es", "MX")

    fun todayEpochDay(): Long {
        val local = Calendar.getInstance()
        return ymdToEpochDay(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun ymdToEpochDay(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, monthZeroBased, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return utc.timeInMillis / MILLIS_PER_DAY
    }

    fun utcMillisToEpochDay(utcMillis: Long): Long = utcMillis / MILLIS_PER_DAY

    fun epochDayToUtcMillis(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

    fun formatEpochDay(epochDay: Long): String {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochDay * MILLIS_PER_DAY
        }
        val formatter = SimpleDateFormat("d MMM yyyy", spanishLocale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(utc.time)
    }

    fun formatEpochDayShort(epochDay: Long): String {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochDay * MILLIS_PER_DAY
        }
        val formatter = SimpleDateFormat("d MMM", spanishLocale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(utc.time)
    }

    fun formatFollowUpRelative(epochDay: Long, today: Long = todayEpochDay()): String {
        val diff = epochDay - today
        return when {
            diff < -1 -> "Vencido hace ${-diff} días (${formatEpochDayShort(epochDay)})"
            diff == -1L -> "Vencido ayer (${formatEpochDayShort(epochDay)})"
            diff == 0L -> "Hoy"
            diff == 1L -> "Mañana (${formatEpochDayShort(epochDay)})"
            diff in 2..6 -> "En $diff días (${formatEpochDayShort(epochDay)})"
            else -> formatEpochDay(epochDay)
        }
    }

    fun formatTodayHeader(today: Long = todayEpochDay()): String {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = today * MILLIS_PER_DAY
        }
        val formatter = SimpleDateFormat("EEEE d 'de' MMMM", spanishLocale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val raw = formatter.format(utc.time)
        return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(spanishLocale) else it.toString() }
    }

    fun formatDateTimeMillis(millis: Long): String {
        val formatter = SimpleDateFormat("d MMM yyyy · HH:mm", spanishLocale)
        return formatter.format(Date(millis))
    }

    fun formatFileTimestamp(millis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        return formatter.format(Date(millis))
    }

    fun formatDashboardDate(today: Long = todayEpochDay()): String {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = today * MILLIS_PER_DAY
        }
        val formatter = SimpleDateFormat("EEEE, MMM d", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(utc.time)
    }

    fun formatEntryDate(millis: Long): String {
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
        return formatter.format(Date(millis))
    }

    fun isCurrentMonth(millis: Long): Boolean {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.MONTH) == target.get(Calendar.MONTH)
    }
}
