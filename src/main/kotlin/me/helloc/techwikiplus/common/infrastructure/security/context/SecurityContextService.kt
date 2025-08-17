package me.helloc.techwikiplus.common.infrastructure.security.context

import me.helloc.techwikiplus.user.domain.model.UserId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class SecurityContextService {
    fun getCurrentUserId(): UserId? {
        val authentication = SecurityContextHolder.getContext().authentication

        return when (val principal = authentication?.principal) {
            is UserId -> principal
            else -> null
        }
    }

    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null && authentication.isAuthenticated
    }

    fun hasRole(role: String): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.authorities?.any {
            it.authority == "ROLE_$role"
        } ?: false
    }
}
