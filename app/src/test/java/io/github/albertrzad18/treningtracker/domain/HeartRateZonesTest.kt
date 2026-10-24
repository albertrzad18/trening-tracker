package io.github.albertrzad18.treningtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateZonesTest {
    private val zones = heartRateZones(200)

    @Test
    fun zonesCoverFiftyToHundredPercent() {
        assertEquals(5, zones.size)
        assertEquals(100, zones.first().fromBpm)
        assertEquals(120, zones.first().toBpm)
        assertEquals(180, zones.last().fromBpm)
        assertEquals(200, zones.last().toBpm)
    }

    @Test
    fun zoneOfPicksTheRightZone() {
        assertNull(zoneOf(80.0, zones))
        assertEquals(1, zoneOf(105.0, zones))
        assertEquals(3, zoneOf(145.0, zones))
        assertEquals(5, zoneOf(195.0, zones))
        assertEquals(5, zoneOf(210.0, zones))
    }

    @Test
    fun timeInZonesUsesGapsBetweenSamples() {
        val samples = listOf(
            SamplePoint(0, 105.0), // strefa 1 przez 10 s
            SamplePoint(10_000, 145.0), // strefa 3 przez 10 s
            SamplePoint(20_000, 145.0), // ostatnia próbka dostaje typowy odstęp, czyli 10 s
        )
        val times = timeInZones(samples, zones)
        assertEquals(10, times[0].seconds)
        assertEquals(20, times[2].seconds)
        assertEquals(0, times[4].seconds)
    }

    @Test
    fun timeInZonesHandlesTooFewSamples() {
        assertTrue(timeInZones(listOf(SamplePoint(0, 150.0)), zones).all { it.isZero })
        assertTrue(timeInZones(emptyList(), zones).all { it.isZero })
    }

    @Test
    fun downsampleKeepsShortSeriesAndShortensLongOnes() {
        val short = (1..10).map { SamplePoint(it.toLong(), it.toDouble()) }
        assertEquals(short, downsample(short, maxPoints = 50))

        val long = (1..1000).map { SamplePoint(it.toLong(), it.toDouble()) }
        val result = downsample(long, maxPoints = 100)
        assertTrue(result.size <= 100)
        assertEquals(1L, result.first().timeMillis)
        // Średnia pierwszego koszyka (1..10) to 5,5.
        assertEquals(5.5, result.first().value, 0.001)
    }
}
