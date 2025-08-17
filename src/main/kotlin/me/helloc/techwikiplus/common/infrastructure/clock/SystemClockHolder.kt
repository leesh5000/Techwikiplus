package me.helloc.techwikiplus.common.infrastructure.clock

import me.helloc.techwikiplus.user.domain.service.port.ClockHolder
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class SystemClockHolder(
    private val clock: Clock = Clock.systemDefaultZone(),
) : ClockHolder {
    override fun now(): Instant {
        return Instant.now(clock)
    }

    override fun nowEpochMilli(): Long {
        return now().toEpochMilli()
    }

    override fun nowEpochSecond(): Int {
        return now().epochSecond.toInt()
    }
}
