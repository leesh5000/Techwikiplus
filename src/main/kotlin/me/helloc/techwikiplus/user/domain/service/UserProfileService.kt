package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.dto.response.UserProfileResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class UserProfileService(
    private val userReader: UserReader,
) {
    fun getUserProfile(userId: UserId): UserProfileResponse {
        val user: User = userReader.getActiveUser(userId)
        return UserProfileResponse(
            userId = user.id.value.toString(),
            email = user.email.value,
            nickname = user.nickname.value,
            role = user.role.name,
            status = user.status.name,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
    }
}
