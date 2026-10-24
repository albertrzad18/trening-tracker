package io.github.albertrzad18.treningtracker.ui.debug

import android.content.res.Resources
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.data.healthconnect.DataKind
import io.github.albertrzad18.treningtracker.data.healthconnect.RouteState
import io.github.albertrzad18.treningtracker.data.healthconnect.SessionProbe
import io.github.albertrzad18.treningtracker.domain.Discipline

private fun Discipline.labelRes(): Int = when (this) {
    Discipline.POOL -> R.string.discipline_pool
    Discipline.ROAD -> R.string.discipline_road
    Discipline.TRAINER -> R.string.discipline_trainer
}

/** Zamienia wyniki sprawdzania treningów na tekst raportu (do ekranu i do skopiowania). */
fun buildReport(probes: List<SessionProbe>, days: Int, res: Resources): String = buildString {
    appendLine(res.getString(R.string.report_header, probes.size, days))
    for (discipline in Discipline.entries) {
        val group = probes.filter { it.discipline == discipline }
        appendLine()
        appendLine(res.getString(R.string.report_discipline, res.getString(discipline.labelRes()), group.size))
        if (group.isEmpty()) continue
        for (kind in DataKind.entries) {
            val withData = group.count { (it.recordCounts[kind] ?: 0) > 0 }
            appendLine("  ${kind.name}: $withData/${group.size}")
        }
        appendLine("  ${res.getString(R.string.report_segments, group.count { it.segments > 0 }, group.size)}")
        appendLine("  ${res.getString(R.string.report_laps, group.count { it.laps > 0 }, group.size)}")
        appendLine(
            "  ${res.getString(
                R.string.report_route,
                group.count { it.route == RouteState.DATA },
                group.count { it.route == RouteState.CONSENT_REQUIRED },
                group.size,
            )}",
        )
        appendLine("  ${res.getString(R.string.report_origins, group.map { it.origin }.distinct().joinToString())}")
    }
    val others = probes.filter { it.discipline == null }.groupingBy { it.exerciseType }.eachCount()
    if (others.isNotEmpty()) {
        appendLine()
        appendLine(res.getString(R.string.report_other_types))
        others.entries.sortedByDescending { it.value }.forEach { (type, count) ->
            appendLine("  type=$type: $count")
        }
    }
}
