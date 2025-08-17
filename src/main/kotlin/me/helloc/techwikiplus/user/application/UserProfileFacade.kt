package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.interfaces.web.port.UserProfileUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class UserProfileFacade(
    private val userReader: UserReader,
) : UserProfileUseCase {
    override fun execute(userId: UserId): UserProfileUseCase.Result {
        val user = userReader.get(userId)
        return UserProfileUseCase.Result(
            userId = user.id,
            email = user.email.value,
            nickname = user.nickname.value,
            role = user.role,
            status = user.status,
            createdAt = user.createdAt,
            modifiedAt = user.modifiedAt,
        )
    }
}
