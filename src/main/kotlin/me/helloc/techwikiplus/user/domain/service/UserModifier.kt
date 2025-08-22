package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.service.port.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserModifier(
    private val clockHolder: ClockHolder,
    private val repository: UserRepository,
) {
    fun activate(user: User): User {
        val updatedAt: Instant = clockHolder.now()
        val activatedUser: User = user.activate(updatedAt)
        return repository.save(activatedUser)
    }

    fun setPending(user: User): User {
        val updatedAt: Instant = clockHolder.now()
        val pendingUser: User = user.setPending(updatedAt)
        return repository.save(pendingUser)
    }
}
