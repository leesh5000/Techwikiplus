package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.tag.TagId

interface TagIdGenerator {
    fun next(): TagId
}
