package me.helloc.techwikiplus.common.infrastructure.id

import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.service.port.PostReviewIdGenerator
import org.springframework.stereotype.Component

@Component
class SnowflakePostReviewIdGenerator(
    private val snowflake: Snowflake,
) : PostReviewIdGenerator {
    override fun generate(): PostReviewId {
        return PostReviewId(snowflake.nextId())
    }
}
