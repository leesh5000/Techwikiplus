package me.helloc.techwikiplus.common.config.testcontainers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * Redis TestContainer 싱글톤 관리
 *
 * 테스트 실행 시 하나의 Redis 컨테이너를 재사용하여 성능을 향상시킵니다.
 * 컨테이너는 JVM이 종료될 때까지 유지되며, 모든 통합 테스트에서 공유됩니다.
 */
object RedisTestContainer {
    private const val REDIS_VERSION = "redis:6.2-alpine"
    private const val REDIS_PORT = 6379

    val instance: GenericContainer<*> by lazy {
        GenericContainer(DockerImageName.parse(REDIS_VERSION))
            .withExposedPorts(REDIS_PORT)
            .withReuse(true)
            .withLabel("testcontainers.reuse.enable", "true")
            // 컨테이너 중지 시 즉시 제거 설정
            .withCommand("redis-server", "--save", "\"\"", "--appendonly", "no")
            .apply {
                start()
                // JVM 종료 시 컨테이너 정리를 위한 shutdown hook 추가
                Runtime.getRuntime().addShutdownHook(
                    Thread {
                        try {
                            if (isRunning) {
                                stop()
                            }
                        } catch (e: Exception) {
                            // 종료 중 발생한 예외는 무시
                        }
                    },
                )
            }
    }

    /**
     * Spring Boot가 사용할 Redis 설정을 동적으로 제공
     */
    fun getProperties(): Map<String, String> =
        mapOf(
            "spring.data.redis.host" to instance.host,
            "spring.data.redis.port" to instance.getMappedPort(REDIS_PORT).toString(),
            "spring.data.redis.password" to "",
        )
}
