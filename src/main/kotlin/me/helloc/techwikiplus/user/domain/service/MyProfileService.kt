package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.dto.response.UserProfileResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class MyProfileService(
    private val userReader: UserReader,
    private val authorizationService: UserAuthorizationService,
) {
    fun getMyProfile(): UserProfileResponse {
        val currentUserId = authorizationService.getCurrentUserOrThrow()
        val user = userReader.getActiveUser(currentUserId)
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
