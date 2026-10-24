package io.github.albertrzad18.treningtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTest {
    private fun point(lat: Double, lon: Double, alt: Double? = null, time: Long = 0) =
        RoutePointDomain(time, lat, lon, alt)

    @Test
    fun distanceBetweenTwoPointsIsRealistic() {
        // Jeden stopień szerokości geograficznej to około 111 km.
        val meters = distanceM(point(50.0, 20.0), point(51.0, 20.0))
        assertEquals(111_000.0, meters, 500.0)
    }

    @Test
    fun elevationProfileAddsUpDistance() {
        val track = RouteTrack(
            listOf(
                point(50.0, 20.0, 200.0),
                point(50.01, 20.0, 220.0),
                point(50.02, 20.0, 210.0),
            ),
        )
        val profile = track.elevationProfile
        assertEquals(3, profile.size)
        assertEquals(0.0, profile[0].first, 0.001)
        assertEquals(200.0, profile[0].second, 0.001)
        // Każdy krok to około 1,11 km.
        assertEquals(1.11, profile[1].first, 0.05)
        assertEquals(2.22, profile[2].first, 0.05)
    }

    @Test
    fun trackWithoutAltitudeHasNoProfile() {
        val track = RouteTrack(listOf(point(50.0, 20.0), point(50.01, 20.0)))
        assertTrue(track.elevationProfile.isEmpty())
        assertTrue(!track.isEmpty)
    }

    @Test
    fun singlePointIsNotARoute() {
        assertTrue(RouteTrack(listOf(point(50.0, 20.0))).isEmpty)
        assertTrue(RouteTrack(emptyList()).isEmpty)
    }
}
