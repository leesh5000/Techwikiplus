package me.helloc.techwikiplus.common.infrastructure.id

import me.helloc.techwikiplus.post.domain.model.review.ReviewCommentId
import me.helloc.techwikiplus.post.domain.service.port.ReviewCommentIdGenerator
import org.springframework.stereotype.Component

@Component
class SnowflakeReviewCommentIdGenerator(
    private val snowflake: Snowflake,
) : ReviewCommentIdGenerator {
    override fun next(): ReviewCommentId {
        return ReviewCommentId(snowflake.nextId())
    }
}
