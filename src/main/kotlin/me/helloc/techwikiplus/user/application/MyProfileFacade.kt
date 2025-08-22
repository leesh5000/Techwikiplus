package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.service.UserAuthorizationService
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.interfaces.web.port.MyProfileUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class MyProfileFacade(
    private val userReader: UserReader,
    private val authorizationService: UserAuthorizationService,
) : MyProfileUseCase {
    override fun execute(): MyProfileUseCase.Result {
        val currentUserId = authorizationService.getCurrentUserOrThrow()
        val user = userReader.get(currentUserId)

        return MyProfileUseCase.Result(
            userId = user.id,
            email = user.email.value,
            nickname = user.nickname.value,
            role = user.role,
            status = user.status,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
    }
}
