package me.helloc.techwikiplus.user.domain.service.port

import me.helloc.techwikiplus.user.domain.model.UserId

interface UserIdGenerator {
    fun next(): UserId
}
