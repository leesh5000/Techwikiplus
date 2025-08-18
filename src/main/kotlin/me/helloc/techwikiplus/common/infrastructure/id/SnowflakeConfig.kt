package me.helloc.techwikiplus.common.infrastructure.id

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SnowflakeConfig {
    companion object {
        private val logger = LoggerFactory.getLogger(SnowflakeConfig::class.java)
    }

    /**
     * 애플리케이션 전체에서 공유되는 단일 Snowflake 인스턴스를 생성합니다.
     *
     * nodeId는 다음 우선순위로 결정됩니다:
     * 1. 시스템 프로퍼티 'snowflake.node.id'
     * 2. 환경변수 'SNOWFLAKE_NODE_ID'
     * 3. 기본값 0
     */
    @Bean
    fun snowflake(): Snowflake {
        val configuration = SnowflakeConfiguration.fromEnvironment()
        logger.info(
            "Snowflake ID Generator initialized with nodeId: {} (source: {})",
            configuration.nodeId,
            when {
                System.getProperty("snowflake.node.id") != null -> "System Property"
                System.getenv("SNOWFLAKE_NODE_ID") != null -> "Environment Variable"
                else -> "Default Value"
            },
        )

        // 운영 환경에서 기본값 사용 시 경고
        if (configuration.nodeId == 0L && isProductionEnvironment()) {
            logger.warn(
                "⚠️ Snowflake is using default nodeId=0. " +
                    "In multi-instance deployments, set unique SNOWFLAKE_NODE_ID for each instance!",
            )
        }

        return Snowflake(configuration)
    }

    @PostConstruct
    fun logInitialization() {
        logger.info("Snowflake configuration loaded. Multi-instance deployments require unique nodeId per instance.")
    }

    private fun isProductionEnvironment(): Boolean {
        val activeProfile = System.getProperty("spring.profiles.active") ?: ""
        return activeProfile.contains("prod") || activeProfile.contains("production")
    }
}
