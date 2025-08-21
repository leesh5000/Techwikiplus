package me.helloc.techwikiplus.common.infrastructure.id

import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.port.PostIdGenerator
import org.springframework.stereotype.Component

@Component
class SnowflakePostIdGenerator(
    private val snowflake: Snowflake,
) : PostIdGenerator {
    override fun next(): PostId {
        return PostId(snowflake.nextId())
    }
}
