package me.helloc.techwikiplus.common.infrastructure.persistence

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.Nickname
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.service.port.UserRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.UserJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.UserEntity
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.mapper.UserEntityMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
    private val mapper: UserEntityMapper,
) : UserRepository {
    override fun findBy(userId: UserId): User? {
        return jpaRepository.findById(userId.value)
            .orElse(null)
            ?.let {
                mapper.toDomain(it)
            }
    }

    override fun findBy(email: Email): User? {
        return jpaRepository.findByEmail(email.value)
            ?.let {
                mapper.toDomain(it)
            }
    }

    override fun exists(email: Email): Boolean {
        return jpaRepository.existsByEmail(email.value)
    }

    override fun exists(nickname: Nickname): Boolean {
        return jpaRepository.existsByNicknameIgnoreCase(nickname.value)
    }

    @Transactional
    override fun save(user: User): User {
        val entity = mapper.toEntity(user)
        val savedEntity = jpaRepository.save<UserEntity>(entity)
        return mapper.toDomain(savedEntity)
    }
}
