package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId

interface PostRevisionIdGenerator {
    fun generate(): PostRevisionId
}
