package io.github.albertrzad18.treningtracker.ui.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import io.github.albertrzad18.treningtracker.BuildConfig
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.RouteTrack
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline

/**
 * Mapa trasy na kafelkach OpenStreetMap (bez klucza API).
 * Mapa to zwykły widok Androida, więc wkładamy ją w Compose przez [AndroidView].
 * Ślad jest cienki i w kolorze dyscypliny, a początek i koniec dostają własne znaczniki.
 */
@Composable
fun RouteMap(track: RouteTrack, discipline: Discipline, modifier: Modifier = Modifier, height: Dp = 220.dp) {
    val context = LocalContext.current
    val colors = LocalTtColors.current
    val lineColor = colors.of(discipline).toArgb()
    val backgroundColor = colors.surface.toArgb()
    val inkColor = colors.ink.toArgb()
    val density = LocalDensity.current
    val strokePx = with(density) { 3.dp.toPx() }
    val markerPx = with(density) { 6.dp.toPx() }
    val labelPx = with(density) { 11.dp.toPx() }
    val startLabel = stringResource(R.string.detail_route_start)
    val endLabel = stringResource(R.string.detail_route_finish)

    DisposableEffect(Unit) {
        // Serwery OSM wymagają własnej nazwy klienta, inaczej blokują ruch.
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = BuildConfig.APPLICATION_ID
            // Kafelki trzymamy w pamięci podręcznej apki, więc nie potrzeba uprawnień do plików.
            osmdroidBasePath = context.cacheDir
            osmdroidTileCache = context.cacheDir.resolve("osm")
        }
        onDispose { }
    }

    AndroidView(
        // Bez przycięcia mapa rysuje kafelki poza swoim miejscem i zasłania sąsiednie panele.
        modifier = modifier.fillMaxWidth().height(height).clip(TtShape.map),
        factory = { ctx -> buildMapView(ctx) },
        update = { map ->
            map.overlays.clear()
            val points = track.points.map { GeoPoint(it.latitude, it.longitude) }
            if (points.isNotEmpty()) {
                map.overlays.add(
                    Polyline(map).apply {
                        setPoints(points)
                        outlinePaint.strokeWidth = strokePx
                        outlinePaint.color = lineColor
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                    },
                )
                map.overlays.add(
                    EndpointsOverlay(
                        start = points.first(),
                        end = points.last(),
                        lineColor = lineColor,
                        backgroundColor = backgroundColor,
                        inkColor = inkColor,
                        radius = markerPx,
                        textSize = labelPx,
                        startLabel = startLabel,
                        endLabel = endLabel,
                    ),
                )
                // `zoomToBoundingBox` działa dopiero, gdy widok zna swój rozmiar.
                map.post {
                    map.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(points).increaseByScale(1.2f), false)
                }
            }
            map.invalidate()
        },
        onRelease = { it.onDetach() },
    )
}

private fun buildMapView(context: Context): MapView = MapView(context).apply {
    setTileSource(TileSourceFactory.MAPNIK)
    setMultiTouchControls(true)
    // Bez tego na niektórych wersjach widać dwa paski przybliżania.
    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
}

/**
 * Znaczniki początku i końca trasy: start to kółko wypełnione tłem z obwódką w kolorze dyscypliny,
 * meta jest wypełniona kolorem. Rysujemy je same, żeby nie brać domyślnych pinezek osmdroid.
 */
private class EndpointsOverlay(
    private val start: GeoPoint,
    private val end: GeoPoint,
    private val lineColor: Int,
    private val backgroundColor: Int,
    private val inkColor: Int,
    private val radius: Float,
    private val textSize: Float,
    private val startLabel: String,
    private val endLabel: String,
) : Overlay() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = radius / 2.4f
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = this@EndpointsOverlay.textSize
        isFakeBoldText = true
    }

    override fun draw(canvas: Canvas, map: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = map.projection
        val startPoint = projection.toPixels(start, null)
        val endPoint = projection.toPixels(end, null)

        fill.color = backgroundColor
        canvas.drawCircle(startPoint.x.toFloat(), startPoint.y.toFloat(), radius, fill)
        stroke.color = lineColor
        canvas.drawCircle(startPoint.x.toFloat(), startPoint.y.toFloat(), radius, stroke)

        fill.color = lineColor
        canvas.drawCircle(endPoint.x.toFloat(), endPoint.y.toFloat(), radius, fill)
        stroke.color = backgroundColor
        canvas.drawCircle(endPoint.x.toFloat(), endPoint.y.toFloat(), radius, stroke)

        drawLabel(canvas, startLabel, startPoint.x.toFloat(), startPoint.y.toFloat())
        drawLabel(canvas, endLabel, endPoint.x.toFloat(), endPoint.y.toFloat())
    }

    private fun drawLabel(canvas: Canvas, text: String, x: Float, y: Float) {
        val width = label.measureText(text) + radius * 2
        val top = y - radius * 3.4f
        fill.color = backgroundColor
        canvas.drawRoundRect(
            x - width / 2,
            top - textSize,
            x + width / 2,
            top + textSize * 0.4f,
            radius,
            radius,
            fill,
        )
        label.color = inkColor
        canvas.drawText(text, x, top, label)
    }
}
