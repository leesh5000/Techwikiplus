package me.helloc.techwikiplus.common.infrastructure

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import java.time.Instant

class FakeClockHolder(
    private var fixedInstant: Instant = Instant.parse("2025-01-01T00:00:00Z"),
) : ClockHolder {
    override fun now(): Instant {
        return fixedInstant
    }

    override fun nowEpochMilli(): Long {
        return fixedInstant.toEpochMilli()
    }

    override fun nowEpochSecond(): Int {
        return fixedInstant.epochSecond.toInt()
    }

    fun setFixedTime(instant: Instant) {
        this.fixedInstant = instant
    }

    fun advanceTimeBySeconds(seconds: Long) {
        this.fixedInstant = fixedInstant.plusSeconds(seconds)
    }

    fun advanceTimeByMinutes(minutes: Long) {
        this.fixedInstant = fixedInstant.plusSeconds(minutes * 60)
    }
}
