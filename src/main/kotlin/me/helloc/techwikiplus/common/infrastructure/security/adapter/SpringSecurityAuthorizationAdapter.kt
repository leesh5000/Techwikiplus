package me.helloc.techwikiplus.common.infrastructure.security.adapter

import me.helloc.techwikiplus.common.domain.service.port.AuthorizationPort
import me.helloc.techwikiplus.common.infrastructure.security.context.SecurityContextService
import me.helloc.techwikiplus.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole
import org.springframework.stereotype.Component

@Component
class SpringSecurityAuthorizationAdapter(
    private val securityContextService: SecurityContextService,
) : AuthorizationPort {
    override fun getCurrentUserId(): UserId? {
        return securityContextService.getCurrentUserId()
    }

    override fun requireAuthenticated(): UserId {
        return getCurrentUserId() ?: throw UserDomainException(UserErrorCode.UNAUTHORIZED)
    }

    override fun isAuthenticated(): Boolean {
        return securityContextService.isAuthenticated()
    }

    override fun hasRole(role: UserRole): Boolean {
        return securityContextService.hasRole(role.name)
    }

    override fun canAccessUser(targetUserId: UserId): Boolean {
        val currentUserId = getCurrentUserId() ?: return false

        return when {
            hasRole(UserRole.ADMIN) -> true
            currentUserId == targetUserId -> true
            else -> false
        }
    }
}
