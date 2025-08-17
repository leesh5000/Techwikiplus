package me.helloc.techwikiplus.user.domain.service.port

import java.time.Instant

interface ClockHolder {
    fun now(): Instant

    fun nowEpochMilli(): Long

    fun nowEpochSecond(): Int
}
