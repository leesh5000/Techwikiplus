package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.domain.service.port.AuthorizationPort
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.user.domain.model.UserRole
import org.springframework.stereotype.Service

@Service
class PostAuthorizationService(
    private val authorizationPort: AuthorizationPort,
) {
    fun requireAdminRole() {
        if (!authorizationPort.hasRole(UserRole.ADMIN)) {
            throw PostDomainException(PostErrorCode.FORBIDDEN_POST_ROLE)
        }
    }
}
