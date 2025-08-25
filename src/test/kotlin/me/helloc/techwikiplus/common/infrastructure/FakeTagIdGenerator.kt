package me.helloc.techwikiplus.common.infrastructure

import me.helloc.techwikiplus.post.domain.model.tag.TagId
import me.helloc.techwikiplus.post.domain.service.port.TagIdGenerator
import java.util.concurrent.atomic.AtomicLong

class FakeTagIdGenerator(
    private val startId: Long = 1L,
) : TagIdGenerator {
    private val idCounter = AtomicLong(startId)

    override fun next(): TagId {
        return TagId(idCounter.getAndIncrement())
    }

    fun reset(startId: Long = 1L) {
        idCounter.set(startId)
    }
}
