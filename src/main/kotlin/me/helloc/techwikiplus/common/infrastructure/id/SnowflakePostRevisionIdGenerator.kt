package me.helloc.techwikiplus.common.infrastructure.id

import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionIdGenerator
import org.springframework.stereotype.Component

@Component
class SnowflakePostRevisionIdGenerator(
    private val snowflake: Snowflake,
) : PostRevisionIdGenerator {
    override fun generate(): PostRevisionId {
        return PostRevisionId(snowflake.nextId())
    }
}
