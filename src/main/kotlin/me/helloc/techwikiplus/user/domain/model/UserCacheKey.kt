package me.helloc.techwikiplus.user.domain.model

enum class UserCacheKey(val keyFormat: String) {
    REGISTRATION_CODE_KEY_PREFIX("user:registration_code:%s"),
}
