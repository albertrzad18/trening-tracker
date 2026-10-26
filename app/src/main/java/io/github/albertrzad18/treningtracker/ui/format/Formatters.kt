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

/** Zaokrąglamy „od połowy w górę” (1,25 → 1,3). Domyślne HALF_EVEN dawało 1,2 i rozjeżdżało się z zegarkiem. */
private fun number(pattern: String, value: Double): String =
    DecimalFormat(pattern, symbols()).apply { roundingMode = java.math.RoundingMode.HALF_UP }.format(value)

/** `1 250 m`: pojedynczy trening na basenie liczymy w metrach, tak jak na zegarku. */
fun formatDistanceM(meters: Double): String = number("#,##0", meters) + " m"

/** Dystans basenu w metrach (pojedynczy trening i sumy: liczy się każde 25 m), reszty w km. */
fun formatWorkoutDistance(discipline: io.github.albertrzad18.treningtracker.domain.Discipline, meters: Double): String =
    if (discipline == io.github.albertrzad18.treningtracker.domain.Discipline.POOL) formatDistanceM(meters) else formatDistanceKm(meters)

/** To samo co [formatWorkoutDistance], rozbite na liczbę i jednostkę (do dużych kafelków). */
fun workoutDistanceParts(discipline: io.github.albertrzad18.treningtracker.domain.Discipline, meters: Double) =
    if (discipline == io.github.albertrzad18.treningtracker.domain.Discipline.POOL) {
        ValueUnit(number("#,##0", meters), "m")
    } else {
        distanceParts(meters)
    }

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

/**
 * Polska odmiana rzeczownika po liczbie: `1 dzień`, `3 dni`, `7 dni`, `22 dni`.
 * Robimy to sami, bo `plurals` w Androidzie idzie za językiem telefonu, a interfejs jest zawsze po polsku.
 */
fun pluralPl(count: Int, one: String, few: String, many: String): String {
    val lastTwo = count % 100
    val last = count % 10
    val form = when {
        count == 1 -> one
        last in 2..4 && lastTwo !in 12..14 -> few
        else -> many
    }
    return "$count $form"
}

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

// --- Liczba i jednostka osobno (duża liczba + mała jednostka obok) ---

/** Liczba i jej jednostka osobno: wartość rysujemy dużym stopniem, jednostkę małym i w ink2. */
data class ValueUnit(val value: String, val unit: String?)

/** `3,8` + `km` */
fun distanceParts(meters: Double) = ValueUnit(number("#,##0.0", meters / 1000.0), "km")

/** `1 793` + `kcal` */
fun caloriesParts(kcal: Double) = ValueUnit(number("#,##0", kcal), "kcal")

/** `138` + `bpm` */
fun heartRateParts(bpm: Long) = ValueUnit(bpm.toString(), "bpm")

/** `26,8` + `km/h` */
fun speedParts(kmh: Double) = ValueUnit(number("0.0", kmh), "km/h")

/** `2:15` + `/100 m` */
fun paceParts(secondsPer100m: Double): ValueUnit {
    val total = secondsPer100m.roundToInt()
    return ValueUnit("%d:%02d".format(total / 60, total % 60), "/100\u00a0m")
}

/** `326` + `m` */
fun elevationParts(meters: Double) = ValueUnit(number("#,##0", meters), "m")

/** `326 m` */
fun formatElevation(meters: Double): String = number("#,##0", meters) + "\u00a0m"

// --- Krótkie daty ---

private fun monthAbbr(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("LLL", PL))

/** `22 paź` (bez roku, do wiersza listy). */
fun formatDayMonth(date: LocalDate): String = "${date.dayOfMonth} ${monthAbbr(date)}"

/** `pn`, `wt`, `śr`, `czw`, `pt`, `sob`, `nd` — własne skróty, bo systemowe (`niedz.`) są za długie na wąską kolumnę. */
fun formatDayOfWeekShort(date: LocalDate): String = when (date.dayOfWeek) {
    java.time.DayOfWeek.MONDAY -> "pn"
    java.time.DayOfWeek.TUESDAY -> "wt"
    java.time.DayOfWeek.WEDNESDAY -> "śr"
    java.time.DayOfWeek.THURSDAY -> "czw"
    java.time.DayOfWeek.FRIDAY -> "pt"
    java.time.DayOfWeek.SATURDAY -> "sob"
    java.time.DayOfWeek.SUNDAY -> "nd"
}

/** `19–25 paź`, a przy przełomie miesiąca `28 wrz – 4 paź`. */
fun formatRangeShort(start: LocalDate, endInclusive: LocalDate): String =
    if (start.month == endInclusive.month && start.year == endInclusive.year) {
        "${start.dayOfMonth}–${endInclusive.dayOfMonth} ${monthAbbr(endInclusive)}"
    } else {
        "${formatDayMonth(start)} – ${formatDayMonth(endInclusive)}"
    }

/** `19 paź 2026 – 25 paź 2026` */
fun formatRangeFull(start: LocalDate, endInclusive: LocalDate): String =
    "${formatDate(start)} – ${formatDate(endInclusive)}"

/** `37%` — sama wielkość zmiany; znak pokazuje strzałka. */
fun formatPercentAbs(percent: Double): String = "${kotlin.math.abs(percent).roundToInt()}%"
