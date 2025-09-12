package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.history.PostHistoryId

interface PostHistoryIdGenerator {
    fun generate(): PostHistoryId
}
