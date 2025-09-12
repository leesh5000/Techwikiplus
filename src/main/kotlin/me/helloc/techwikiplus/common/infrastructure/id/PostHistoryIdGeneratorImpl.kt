package me.helloc.techwikiplus.common.infrastructure.id

import me.helloc.techwikiplus.post.domain.model.history.PostHistoryId
import me.helloc.techwikiplus.post.domain.service.port.PostHistoryIdGenerator
import org.springframework.stereotype.Component

@Component
class PostHistoryIdGeneratorImpl(
    private val snowflake: Snowflake,
) : PostHistoryIdGenerator {
    override fun generate(): PostHistoryId {
        return PostHistoryId(snowflake.nextId())
    }
}
