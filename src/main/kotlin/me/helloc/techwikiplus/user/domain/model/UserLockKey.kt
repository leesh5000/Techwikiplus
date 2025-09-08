package me.helloc.techwikiplus.user.domain.model

enum class UserLockKey(
    val keyFormat: String,
) {
    VERIFY_EMAIL_LOCK_KEY_PREFIX("user:verify-email:%s"),
    SIGN_UP_LOCK_KEY_PREFIX("user:sign-up:%s"),
}
