package me.helloc.techwikiplus.common.config.testcontainers

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

/**
 * TestContainers를 Spring Boot 테스트와 통합하기 위한 초기화 클래스
 *
 * MySQL, Redis 및 MailHog 컨테이너의 동적 설정을 Spring 애플리케이션 컨텍스트에 주입합니다.
 */
class TestContainersInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        // MySQL 컨테이너 시작 및 설정 적용
        val mysqlProperties = MySqlTestContainer.getProperties()

        // Redis 컨테이너 시작 및 설정 적용
        val redisProperties = RedisTestContainer.getProperties()

        // MailHog 컨테이너 시작 및 설정 적용
        val mailhogProperties = MailHogTestContainer.getProperties()

        // JWT 설정 추가 (최소 256비트/32바이트 이상의 시크릿 키 필요)
        val jwtProperties =
            mapOf(
                "jwt.secret" to "test-secret-key-for-e2e-testing-must-be-at-least-32-bytes-long-for-HS256",
                "jwt.access-token-validity-in-seconds" to "3600",
                "jwt.refresh-token-validity-in-seconds" to "2592000",
            )

        // 모든 TestContainer 설정 병합
        val properties = mysqlProperties + redisProperties + mailhogProperties + jwtProperties

        TestPropertyValues.of(properties)
            .applyTo(applicationContext.environment)
    }
}
