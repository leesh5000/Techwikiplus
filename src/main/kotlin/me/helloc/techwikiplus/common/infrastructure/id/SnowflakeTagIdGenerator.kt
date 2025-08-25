package me.helloc.techwikiplus.common.infrastructure.id

import me.helloc.techwikiplus.post.domain.model.tag.TagId
import me.helloc.techwikiplus.post.domain.service.port.TagIdGenerator
import org.springframework.stereotype.Component

@Component
class SnowflakeTagIdGenerator(
    private val snowflake: Snowflake,
) : TagIdGenerator {
    override fun next(): TagId {
        return TagId(snowflake.nextId())
    }
}
