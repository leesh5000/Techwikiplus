package me.helloc.techwikiplus.post.domain.model

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode

class PostId(val value: Long) {
    init {
        if (value <= 0) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.INVALID_POST_ID_FORMAT,
                params = arrayOf("postId"),
            )
        }
        // Snowflake ID는 64비트 정수이므로 Long 타입의 최대값을 넘지 않음
        // 추가 검증이 필요하면 여기에 추가
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PostId) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    companion object {
        fun from(value: String): PostId {
            try {
                return PostId(value.toLong())
            } catch (e: NumberFormatException) {
                throw PostDomainException(
                    postErrorCode = PostErrorCode.INVALID_POST_ID_FORMAT,
                    params = arrayOf(value),
                    cause = e,
                )
            }
        }
    }
}
