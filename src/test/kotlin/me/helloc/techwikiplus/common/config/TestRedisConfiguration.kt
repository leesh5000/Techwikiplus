package me.helloc.techwikiplus.common.config

import io.lettuce.core.ClientOptions
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.Duration

/**
 * 테스트 환경을 위한 Redis 설정
 *
 * Lettuce 클라이언트의 재연결 및 타임아웃 동작을 제어하여
 * 테스트 종료 시 빠른 정리가 가능하도록 설정합니다.
 */
@Configuration
@Profile("e2e-test")
class TestRedisConfiguration {
    /**
     * 테스트용 Lettuce 클라이언트 리소스 설정
     *
     * - 짧은 재연결 지연 시간
     * - 빠른 종료를 위한 설정
     */
    @Bean(destroyMethod = "shutdown")
    fun lettuceClientResources(): ClientResources {
        return DefaultClientResources.builder()
            .ioThreadPoolSize(2) // 테스트용으로 작은 스레드 풀
            .computationThreadPoolSize(2)
            .build()
    }

    /**
     * Lettuce 클라이언트 설정 커스터마이저
     *
     * - 자동 재연결 비활성화
     * - 짧은 명령 타임아웃 설정
     * - 버퍼 사용 제한 비활성화
     */
    @Bean
    fun lettuceClientConfigurationBuilderCustomizer(): LettuceClientConfigurationBuilderCustomizer {
        return LettuceClientConfigurationBuilderCustomizer { clientConfigurationBuilder ->
            clientConfigurationBuilder
                .commandTimeout(Duration.ofSeconds(2)) // 명령 타임아웃 2초
                .shutdownTimeout(Duration.ofMillis(100)) // 종료 타임아웃 100ms
                .clientOptions(
                    ClientOptions.builder()
                        .autoReconnect(false) // 자동 재연결 비활성화
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS) // 연결 끊김 시 명령 거부
                        .cancelCommandsOnReconnectFailure(true) // 재연결 실패 시 명령 취소
                        .suspendReconnectOnProtocolFailure(true) // 프로토콜 실패 시 재연결 중단
                        .requestQueueSize(10) // 작은 요청 큐 크기
                        .build(),
                )
        }
    }
}
