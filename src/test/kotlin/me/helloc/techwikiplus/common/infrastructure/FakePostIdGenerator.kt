package me.helloc.techwikiplus.common.infrastructure

import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.port.PostIdGenerator

class FakePostIdGenerator(
    private var startId: Long = 1000000L,
) : PostIdGenerator {
    private var currentId = startId

    override fun next(): PostId {
        return PostId(currentId++)
    }

    fun reset() {
        currentId = startId
    }

    fun setStartId(id: Long) {
        startId = id
        currentId = id
    }

    fun getCurrentId(): Long {
        return currentId
    }
}
