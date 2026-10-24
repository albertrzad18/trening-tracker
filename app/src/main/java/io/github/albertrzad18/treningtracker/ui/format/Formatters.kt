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

/** `34%` z udziału 0..1 (bez znaku, inaczej niż [formatPercentChange]). */
fun formatPercent(fraction: Double): String = "${(fraction * 100).roundToInt()}%"

// --- Krótkie etykiety na osie wykresów ---

/**
 * Etykieta osi czasu: `0`, `45 min`, `2,5 h`, `25 h`.
 * Przy dużych wartościach rezygnujemy z części dziesiętnych, żeby oś nie była szeroka.
 */
fun formatMinutesShort(minutes: Double): String {
    val hours = minutes / 60.0
    return when {
        minutes < 1 -> "0"
        minutes < 60 -> "${minutes.roundToInt()} min"
        hours < 10 -> number("0.#", hours) + " h"
        else -> "${hours.roundToInt()} h"
    }
}

/** `56 h 29 min` dla sum z wielu treningów; `1:23:45` zostawiamy pojedynczemu treningowi. */
fun formatHoursAndMinutes(duration: Duration): String {
    val minutes = duration.toMinutes()
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours == 0L) "$rest min" else "$hours h $rest min"
}

/** `2:30` – tempo bez jednostki, do wąskiej osi wykresu. */
fun formatPaceShort(secondsPer100m: Double): String {
    val total = secondsPer100m.roundToInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/** `2,4` – jedna cyfra po przecinku, do osi wykresu. */
fun formatDecimalShort(value: Double): String = number("0.0", value)

/** `1 200` bez jednostki: jednostka jest w opisie wykresu, a oś ma być wąska. */
fun formatNumberShort(value: Double): String = number("#,##0", value)

/** `12 km` (bez części dziesiętnych, bo oś ma być wąska). */
fun formatKmShort(km: Double): String = number("#,##0", km) + " km"

/** `24.10` */
fun formatDayShort(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d.MM", PL))

/** `paź` */
fun formatMonthShort(month: java.time.YearMonth): String = month.format(DateTimeFormatter.ofPattern("LLL", PL))

/** `paź 26` dla osi, gdzie mieszają się różne lata. */
fun formatMonthWithYear(month: java.time.YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("LLL yy", PL))
