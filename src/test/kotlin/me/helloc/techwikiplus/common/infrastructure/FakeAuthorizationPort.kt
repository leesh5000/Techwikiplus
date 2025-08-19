package me.helloc.techwikiplus.common.infrastructure

import me.helloc.techwikiplus.common.domain.service.port.AuthorizationPort
import me.helloc.techwikiplus.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole

class FakeAuthorizationPort : AuthorizationPort {
    private var currentUserId: UserId? = null
    private var authenticated: Boolean = false
    private var currentUserRole: UserRole = UserRole.USER

    fun setCurrentUser(
        userId: UserId?,
        role: UserRole = UserRole.USER,
    ) {
        this.currentUserId = userId
        this.authenticated = userId != null
        this.currentUserRole = role
    }

    fun clearCurrentUser() {
        this.currentUserId = null
        this.authenticated = false
        this.currentUserRole = UserRole.USER
    }

    override fun getCurrentUserId(): UserId? {
        return currentUserId
    }

    override fun requireAuthenticated(): UserId {
        return currentUserId ?: throw UserDomainException(UserErrorCode.UNAUTHORIZED)
    }

    override fun isAuthenticated(): Boolean {
        return authenticated
    }

    override fun hasRole(role: UserRole): Boolean {
        return authenticated && currentUserRole == role
    }

    override fun canAccessUser(targetUserId: UserId): Boolean {
        if (!authenticated) return false

        return when {
            currentUserRole == UserRole.ADMIN -> true
            currentUserId == targetUserId -> true
            else -> false
        }
    }
}
