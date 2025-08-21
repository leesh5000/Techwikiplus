package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.post.PostId

interface PostIdGenerator {
    fun next(): PostId
}
