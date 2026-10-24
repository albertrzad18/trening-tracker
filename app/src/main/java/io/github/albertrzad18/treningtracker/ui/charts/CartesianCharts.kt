package io.github.albertrzad18.treningtracker.ui.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent

/** Jedna seria słupków albo linii: kolor i wartości. */
data class ChartSeries(val color: Color, val values: List<Double>)

/** Punkt linii z własnym X (np. numer dnia w roku), gdy serie mają różne długości. */
data class ChartLine(val color: Color, val points: List<Pair<Double, Double>>)

/** Wartość osi X zaokrąglona do czterech miejsc po przecinku: tyle przyjmuje Vico. */
private fun roundX(value: Double): Double = kotlin.math.round(value * 10_000) / 10_000

/** Opis osi X: etykieta dla wartości X i co która ma być pokazana. */
private fun labelFormatter(labels: (Double) -> String) =
    CartesianValueFormatter { _, value, _ -> labels(value) }

/**
 * Słupki ustawione jedne na drugich (każda seria to dyscyplina).
 * Wykres nie przewija się i nie zoomuje: całość ma się zmieścić na ekranie.
 */
@Composable
fun StackedBarChart(
    bars: List<ChartSeries>,
    xLabel: (Int) -> String,
    yLabel: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    labelEvery: Int = 1,
) {
    val count = bars.firstOrNull()?.values?.size ?: 0
    val columns = bars.map { rememberLineComponent(Fill(it.color), thickness = 10.dp, shape = RoundedCornerShape(3.dp)) }
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(bars) {
        producer.runTransaction {
            columnModel {
                bars.forEachIndexed { index, bar -> series(bar.values, key = index) }
            }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(columns),
                mergeMode = { ColumnCartesianLayer.MergeMode.Stacked },
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = labelFormatter(yLabel),
                itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 4 }) },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = labelFormatter { x -> xLabel(x.toInt().coerceIn(0, (count - 1).coerceAtLeast(0))) },
                itemPlacer = remember(labelEvery) { HorizontalAxis.ItemPlacer.aligned(spacing = { labelEvery }) },
            ),
        ),
        modelProducer = producer,
        modifier = modifier.fillMaxWidth(),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content, maxZoom = Zoom.Content),
        chartAreaHeight = height,
    )
}

/** Linie z własnymi wartościami X: trend formy, dystans narastająco. */
@Composable
fun MultiLineChart(
    lines: List<ChartLine>,
    xLabel: (Double) -> String,
    yLabel: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    zeroBased: Boolean = false,
    labelEvery: Int = 1,
) {
    val lineSpecs = lines.map { line ->
        LineCartesianLayer.rememberLine(
            fill = remember(line.color) { LineCartesianLayer.LineFill.single(Fill(line.color)) },
        )
    }
    // Trend tempa kręci się wokół jednej wartości, więc oś zaczyna się tuż pod minimum, a nie od zera.
    val range = remember(lines, zeroBased) {
        if (zeroBased) {
            CartesianLayerRangeProvider.auto()
        } else {
            val values = lines.flatMap { line -> line.points.map { it.second } }
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 0.0
            val margin = ((max - min) * 0.1).takeIf { it > 0 } ?: 1.0
            CartesianLayerRangeProvider.fixed(minY = min - margin, maxY = max + margin)
        }
    }
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(lines) {
        producer.runTransaction {
            lineModel {
                lines.forEachIndexed { index, line ->
                    // Vico przyjmuje na osi X najwyżej cztery miejsca po przecinku, więc zaokrąglamy.
                    series(x = line.points.map { roundX(it.first) }, y = line.points.map { it.second }, key = index)
                }
            }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(lineSpecs),
                rangeProvider = range,
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = labelFormatter(yLabel),
                itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 4 }) },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = labelFormatter(xLabel),
                itemPlacer = remember(labelEvery) { HorizontalAxis.ItemPlacer.aligned(spacing = { labelEvery }) },
            ),
        ),
        modelProducer = producer,
        modifier = modifier.fillMaxWidth(),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content, maxZoom = Zoom.Content),
        chartAreaHeight = height,
    )
}
