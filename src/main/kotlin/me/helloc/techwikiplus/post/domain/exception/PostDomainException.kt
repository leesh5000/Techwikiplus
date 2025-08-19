package me.helloc.techwikiplus.post.domain.exception

open class PostDomainException(
    val postErrorCode: PostErrorCode,
    val params: Array<out Any?> = emptyArray(),
    cause: Throwable? = null,
) : RuntimeException(postErrorCode.name, cause)
