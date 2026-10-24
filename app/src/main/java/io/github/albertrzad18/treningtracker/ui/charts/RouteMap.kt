package io.github.albertrzad18.treningtracker.ui.charts

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.albertrzad18.treningtracker.BuildConfig
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.RouteTrack
import io.github.albertrzad18.treningtracker.ui.common.color
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

/**
 * Mapa trasy na kafelkach OpenStreetMap (bez klucza API).
 * Mapa to zwykły widok Androida, więc wkładamy ją w Compose przez [AndroidView].
 */
@Composable
fun RouteMap(track: RouteTrack, discipline: Discipline, modifier: Modifier = Modifier, height: Dp = 240.dp) {
    val context = LocalContext.current
    val lineColor = discipline.color()

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
        // Bez przycięcia mapa rysuje kafelki poza swoim miejscem i zasłania sąsiednie wykresy.
        modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(12.dp)),
        factory = { ctx -> buildMapView(ctx) },
        update = { map ->
            map.overlays.clear()
            val points = track.points.map { GeoPoint(it.latitude, it.longitude) }
            if (points.isNotEmpty()) {
                map.overlays.add(
                    Polyline(map).apply {
                        setPoints(points)
                        outlinePaint.strokeWidth = 8f
                        outlinePaint.color = lineColor.toArgb()
                    },
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
