package me.helloc.techwikiplus.tools

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Profile

/**
 * 대량 데이터 로더 실행 클래스
 *
 * 실행 방법:
 * 1. IntelliJ에서 이 클래스의 main 함수 실행
 * 2. 또는 터미널에서: ./gradlew bootRun -Pargs="--spring.profiles.active=bulk-loader"
 *
 * 주의사항:
 * - 실행 전 데이터베이스가 실행 중이어야 함
 * - 충분한 디스크 공간 확보 필요 (약 10-20GB)
 * - 실행 시간은 하드웨어에 따라 30-60분 소요
 */
@SpringBootApplication(scanBasePackages = ["me.helloc.techwikiplus"])
@Profile("bulk-loader")
class BulkDataLoaderRunner(
    private val bulkPostDataLoader: BulkPostDataLoader,
) : CommandLineRunner {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun run(vararg args: String?) {
        logger.info("대량 데이터 로더 실행 준비...")

        // 실행 전 현재 데이터 개수 확인
        val currentCount = bulkPostDataLoader.getPostCount()
        logger.info("현재 게시글 수: ${String.format("%,d", currentCount)} 건")

        // 사용자 확인 (실제로는 주석 처리하거나 프로파일로 제어)
        if (currentCount > 0) {
            logger.warn("이미 ${String.format("%,d", currentCount)}개의 게시글이 존재합니다.")
            logger.warn("계속하면 추가로 2000만 건이 삽입됩니다.")

            // 기존 데이터를 삭제하려면 아래 주석 해제
            // bulkPostDataLoader.clearAllPosts()
        }

        // 2000만 건 데이터 삽입 실행
        val targetCount =
            if (args.isNotEmpty() && args[0] != null) {
                args[0]!!.toLongOrNull() ?: 20_000_000L
            } else {
                20_000_000L
            }

        logger.info("목표 삽입 건수: ${String.format("%,d", targetCount)} 건")

        try {
            bulkPostDataLoader.loadPosts(targetCount)

            // 최종 확인
            val finalCount = bulkPostDataLoader.getPostCount()
            logger.info("최종 게시글 수: ${String.format("%,d", finalCount)} 건")
        } catch (e: Exception) {
            logger.error("데이터 로딩 실패", e)
            throw e
        }
    }
}

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", "bulk-loader")
    SpringApplication.run(BulkDataLoaderRunner::class.java, *args)
}
