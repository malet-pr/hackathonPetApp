package com.example.petcare.demo

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class AcceleratedClockTest {
    @Test fun aMinuteAdvancesOneDayAndBackgroundTimeDoesNotAdvance() {
        var elapsed = 100L
        val start = Instant.parse("2026-10-12T11:00:00Z")
        val clock = AcceleratedClock(ZoneId.of("UTC"), start) { elapsed }
        clock.resume()
        elapsed += 1_000
        assertEquals(start.plusSeconds(24 * 60), clock.instant())
        elapsed += 59_000
        assertEquals(start.plusSeconds(24 * 60 * 60), clock.instant())
        clock.pause()
        elapsed += 600_000
        assertEquals(start.plusSeconds(24 * 60 * 60), clock.instant())
        clock.resume()
        elapsed += 20_000
        assertEquals(start.plusSeconds(32 * 60 * 60), clock.instant())
    }
}
