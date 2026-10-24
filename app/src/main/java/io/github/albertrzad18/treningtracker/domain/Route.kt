package io.github.albertrzad18.treningtracker.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Punkt trasy w modelu domenowym. */
data class RoutePointDomain(
    val timeMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double?,
)

/** Trasa treningu: punkty i to, co z nich liczymy (profil wysokości). */
data class RouteTrack(val points: List<RoutePointDomain>) {
    val isEmpty: Boolean get() = points.size < 2

    /** Profil wysokości: dystans od startu w km i wysokość w metrach. Pusty, gdy nie ma wysokości. */
    val elevationProfile: List<Pair<Double, Double>>
        get() {
            if (points.none { it.altitudeM != null }) return emptyList()
            var distance = 0.0
            return points.mapIndexedNotNull { i, point ->
                if (i > 0) distance += distanceM(points[i - 1], point)
                point.altitudeM?.let { (distance / 1000.0) to it }
            }
        }
}

/** Odległość dwóch punktów po powierzchni Ziemi (wzór haversine), w metrach. */
fun distanceM(a: RoutePointDomain, b: RoutePointDomain): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h = sin(dLat / 2) * sin(dLat / 2) + sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)
    return 2 * earthRadius * atan2(sqrt(h), sqrt(1 - h))
}
