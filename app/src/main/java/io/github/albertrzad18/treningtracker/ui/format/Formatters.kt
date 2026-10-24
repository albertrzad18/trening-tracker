package io.github.albertrzad18.treningtracker.ui.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val PL = Locale.forLanguageTag("pl-PL")

private fun symbols() = DecimalFormatSymbols(PL).apply {
    groupingSeparator = ' '
    decimalSeparator = ','
}

private fun number(pattern: String, value: Double): String = DecimalFormat(pattern, symbols()).format(value)

/** `1 234,5 km` (spacja w liczbie to twarda spacja, żeby tekst się nie łamał). */
fun formatDistanceKm(meters: Double): String = number("#,##0.0", meters / 1000.0) + " km"

/** `1:23:45`; zawsze godziny:minuty:sekundy. */
fun formatDuration(duration: Duration): String {
    val total = duration.seconds.coerceAtLeast(0)
    return "%d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
}

/** `24 paź 2026` */
fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMM yyyy", PL))

/** `październik 2026` (nagłówek grupy miesiąca). */
fun formatMonth(month: YearMonth): String = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", PL))

/** Tempo pływackie z sekund na 100 m: `1:45 /100 m`. */
fun formatPacePer100m(secondsPer100m: Double): String {
    val total = secondsPer100m.roundToInt()
    return "%d:%02d /100 m".format(total / 60, total % 60)
}

/** `25,3 km/h` */
fun formatSpeedKmh(kmh: Double): String = number("0.0", kmh) + " km/h"

/** `1 234 kcal` */
fun formatCalories(kcal: Double): String = number("#,##0", kcal) + " kcal"

/** `142 bpm` */
fun formatHeartRate(bpm: Long): String = "$bpm bpm"

/** `+12%` albo `-5%`; `0%` dla zmiany zaokrąglonej do zera. */
fun formatPercentChange(percent: Double): String {
    val rounded = percent.roundToInt()
    return when {
        rounded > 0 -> "+$rounded%"
        else -> "$rounded%"
    }
}

/** `14:30` */
fun formatTime(time: java.time.LocalTime): String = time.format(DateTimeFormatter.ofPattern("HH:mm", PL))

/** `24 paź 2026, 14:30` */
fun formatDateTime(dateTime: java.time.LocalDateTime): String =
    formatDate(dateTime.toLocalDate()) + ", " + formatTime(dateTime.toLocalTime())
