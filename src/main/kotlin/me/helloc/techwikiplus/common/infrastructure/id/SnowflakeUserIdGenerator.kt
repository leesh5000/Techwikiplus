package me.helloc.techwikiplus.common.infrastructure.id

import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.service.port.UserIdGenerator
import org.springframework.stereotype.Component

@Component
class SnowflakeUserIdGenerator(
    private val snowflake: Snowflake,
) : UserIdGenerator {
    override fun next(): UserId {
        return UserId(snowflake.nextId())
    }
}
