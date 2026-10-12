package com.example.petcare.demo

import android.os.SystemClock
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** One simulated day per minute spent in the foreground. Elapsed time never uses wall-clock changes. */
class AcceleratedClock(private val timeZone: ZoneId, start: Instant,
    private val elapsedMillis: () -> Long = { SystemClock.elapsedRealtime() }) : Clock() {
    private var savedMillis = start.toEpochMilli()
    private var resumedAt: Long? = null

    override fun getZone(): ZoneId = timeZone
    override fun withZone(zone: ZoneId): Clock = AcceleratedClock(zone, instant(), elapsedMillis)
    override fun instant(): Instant = synchronized(this) {
        Instant.ofEpochMilli(savedMillis + (resumedAt?.let { elapsedMillis() - it } ?: 0L) * RATIO)
    }

    fun resume() = synchronized(this) {
        if (resumedAt == null) resumedAt = elapsedMillis()
    }

    fun pause() = synchronized(this) {
        resumedAt?.let { savedMillis += (elapsedMillis() - it) * RATIO }
        resumedAt = null
    }

    companion object { const val RATIO = 1440L }
}
