package me.helloc.techwikiplus.common.domain.service.port

import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole

interface AuthorizationPort {
    fun getCurrentUserId(): UserId?

    fun requireAuthenticated(): UserId

    fun isAuthenticated(): Boolean

    fun hasRole(role: UserRole): Boolean

    fun canAccessUser(targetUserId: UserId): Boolean
}
