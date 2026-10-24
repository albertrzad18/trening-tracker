package io.github.albertrzad18.treningtracker.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.labelRes

/**
 * Jedna karta na ekranie Statystyki: tytuł, krótkie wyjaśnienie, wykres.
 * Gdy nie ma czego pokazać ([hasData] = false), zamiast wykresu jest jedno zdanie.
 */
@Composable
fun ChartCard(
    title: String,
    description: String? = null,
    hasData: Boolean = true,
    emptyText: String = stringResource(R.string.chart_no_data),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (hasData) content() else {
                Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Legenda dowolnych serii: kolorowa kropka i podpis. */
@Composable
fun ColorLegend(items: List<Pair<androidx.compose.ui.graphics.Color, String>>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Legenda dyscyplin: kolorowa kropka i nazwa, w stałej kolejności. */
@Composable
fun DisciplineLegend(disciplines: List<Discipline>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        disciplines.forEach { d ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                DisciplineDot(d, size = 10.dp)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(d.labelRes()), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
