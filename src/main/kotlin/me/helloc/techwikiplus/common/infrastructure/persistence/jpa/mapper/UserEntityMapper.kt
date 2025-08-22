package me.helloc.techwikiplus.common.infrastructure.persistence.jpa.mapper

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.UserEntity
import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.user.domain.model.Nickname
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole
import me.helloc.techwikiplus.user.domain.model.UserStatus
import org.springframework.stereotype.Component

@Component
class UserEntityMapper {
    fun toDomain(entity: UserEntity): User {
        return User(
            id = UserId(entity.id),
            email = Email(entity.email),
            nickname = Nickname(entity.nickname),
            encodedPassword = EncodedPassword(entity.password),
            status = UserStatus.valueOf(entity.status),
            role = UserRole.valueOf(entity.role),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    fun toEntity(user: User): UserEntity {
        return UserEntity(
            id = user.id.value,
            email = user.email.value,
            nickname = user.nickname.value,
            password = user.encodedPassword.value,
            status = user.status.name,
            role = user.role.name,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
    }
}
