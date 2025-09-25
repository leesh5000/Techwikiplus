package me.helloc.techwikiplus.post.interfaces.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import me.helloc.techwikiplus.common.infrastructure.id.Snowflake
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import javax.sql.DataSource

@Component
class BulkPostDataLoader(
    private val dataSource: DataSource,
    private val jdbcTemplate: JdbcTemplate,
    private val snowflake: Snowflake,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val BATCH_SIZE = 10_000
        private const val PROGRESS_INTERVAL = 1_000_000
        private const val PARALLEL_WORKERS = 8
        private const val MAX_TAGS_PER_POST = 5

        private val TITLE_TEMPLATES =
            listOf(
                "Spring Boot %d 새로운 기능 소개",
                "Kotlin %d 코루틴 완벽 가이드",
                "React %d 성능 최적화 팁",
                "Docker %d 컨테이너 모범 사례",
                "Kubernetes %d 운영 가이드",
                "PostgreSQL %d 성능 튜닝",
                "Redis %d 캐싱 전략",
                "GraphQL %d API 설계 패턴",
                "Microservices %d 아키텍처 가이드",
                "AWS %d 클라우드 마이그레이션",
                "Jenkins %d CI/CD 파이프라인 구축",
                "Terraform %d 인프라 자동화",
                "Elasticsearch %d 검색 최적화",
                "RabbitMQ %d 메시지 큐 활용",
                "MongoDB %d NoSQL 모범 사례",
                "Vue.js %d 컴포넌트 설계",
                "Angular %d 엔터프라이즈 패턴",
                "Node.js %d 백엔드 최적화",
                "Python %d 데이터 처리 가이드",
                "Java %d 동시성 프로그래밍",
                "TypeScript %d 타입 시스템 활용",
                "Next.js %d SSR/SSG 전략",
                "Nginx %d 로드 밸런싱 설정",
                "MySQL %d 인덱스 최적화",
                "Git %d 브랜칭 전략",
                "OAuth %d 인증 구현 가이드",
                "WebSocket %d 실시간 통신",
                "gRPC %d 마이크로서비스 통신",
                "Kafka %d 스트리밍 처리",
                "Prometheus %d 모니터링 설정",
                "Grafana %d 대시보드 구성",
                "Istio %d 서비스 메시 구현",
                "Flutter %d 크로스플랫폼 개발",
                "Rust %d 시스템 프로그래밍",
                "Go %d 동시성 패턴",
                "Swift %d iOS 개발 팁",
                "Android %d Jetpack Compose",
                "Machine Learning %d 모델 배포",
                "TensorFlow %d 딥러닝 구현",
                "PyTorch %d 신경망 설계",
                "Blockchain %d 스마트 컨트랙트",
                "Ethereum %d DApp 개발",
                "Solidity %d 컨트랙트 보안",
                "WebAssembly %d 성능 최적화",
                "Deno %d 모던 런타임 활용",
                "Svelte %d 반응형 프로그래밍",
                "Tailwind CSS %d 유틸리티 클래스",
                "Webpack %d 번들링 최적화",
                "Vite %d 빌드 도구 활용",
                "Testing %d 자동화 전략",
            )

        private val BODY_TEMPLATES =
            listOf(
                """
                이 문서는 최신 기술 동향과 모범 사례를 다룹니다.
                
                ## 개요
                현대 소프트웨어 개발에서 중요한 개념들을 상세히 설명하고, 실제 프로젝트에 적용할 수 있는 
                실용적인 예제를 제공합니다. 각 섹션은 이론적 배경과 함께 실습 코드를 포함하고 있어 
                즉시 활용 가능합니다.
                
                ## 핵심 개념
                1. 아키텍처 설계 원칙
                2. 성능 최적화 기법
                3. 보안 고려사항
                4. 테스트 전략
                
                ## 구현 예제
                ```kotlin
                class Example {
                    fun demonstrate() {
                        println("실제 구현 코드")
                    }
                }
                ```
                
                ## 성능 벤치마크
                다양한 시나리오에서의 성능 측정 결과를 제시하며, 최적화 전후 비교를 통해 
                개선 효과를 정량적으로 보여줍니다.
                
                ## 결론
                이 가이드를 통해 프로덕션 레벨의 애플리케이션을 구축하는데 필요한 
                핵심 지식을 습득할 수 있습니다.
                """.trimIndent(),
                """
                ## 소개
                이 글에서는 엔터프라이즈 환경에서 검증된 패턴과 실무 경험을 공유합니다.
                
                ### 배경
                대규모 트래픽을 처리하는 시스템 구축 경험을 바탕으로, 실제 운영 환경에서 
                마주치는 다양한 문제들과 해결 방법을 제시합니다.
                
                ### 기술 스택
                - 백엔드: Spring Boot, Kotlin
                - 데이터베이스: PostgreSQL, Redis
                - 인프라: Docker, Kubernetes
                - 모니터링: Prometheus, Grafana
                
                ### 아키텍처 패턴
                마이크로서비스 아키텍처를 기반으로 하며, 각 서비스는 독립적으로 배포 가능한 
                단위로 구성됩니다. 서비스 간 통신은 REST API와 메시지 큐를 활용합니다.
                
                ### 구현 상세
                도메인 주도 설계(DDD) 원칙을 따르며, 각 Bounded Context는 명확한 책임을 가집니다.
                헥사고날 아키텍처를 통해 비즈니스 로직과 인프라스트럭처를 분리합니다.
                
                ### 교훈
                실제 프로젝트를 통해 얻은 교훈과 앞으로의 개선 방향을 제시합니다.
                """.trimIndent(),
                """
                # 기술 문서
                
                ## 목적
                이 문서의 목적은 개발자들이 빠르게 기술을 습득하고 프로젝트에 적용할 수 있도록 
                실용적인 가이드를 제공하는 것입니다.
                
                ## 사전 요구사항
                - 프로그래밍 기초 지식
                - 객체지향 프로그래밍 이해
                - 기본적인 데이터베이스 지식
                
                ## 단계별 가이드
                
                ### Step 1: 환경 설정
                개발 환경을 구성하고 필요한 도구들을 설치합니다.
                
                ### Step 2: 기본 구현
                핵심 기능을 구현하고 테스트합니다.
                
                ### Step 3: 최적화
                성능을 개선하고 코드를 리팩토링합니다.
                
                ### Step 4: 배포
                프로덕션 환경에 배포하고 모니터링을 설정합니다.
                
                ## 트러블슈팅
                자주 발생하는 문제들과 해결 방법을 정리했습니다.
                
                ## 참고 자료
                추가 학습을 위한 유용한 리소스 목록입니다.
                """.trimIndent(),
            )

        private val POST_STATUSES =
            listOf(
                "REVIEWED" to 70,
                "IN_REVIEW" to 20,
                "DRAFT" to 10,
            )

        private val TAG_NAMES =
            listOf(
                "spring", "spring-boot", "kotlin", "java", "javascript", "typescript",
                "react", "vue", "angular", "nodejs", "python", "go", "rust",
                "docker", "kubernetes", "aws", "azure", "gcp", "devops", "ci-cd",
                "microservices", "architecture", "database", "mysql", "postgresql",
                "mongodb", "redis", "kafka", "rabbitmq", "elasticsearch",
                "security", "authentication", "oauth", "jwt", "testing",
                "unit-test", "integration-test", "performance", "optimization",
                "algorithm", "data-structure", "design-pattern", "clean-code",
                "rest-api", "graphql", "grpc", "websocket", "serverless",
                "machine-learning", "ai", "deep-learning", "tensorflow", "pytorch",
                "blockchain", "web3", "ethereum", "smart-contract", "nft",
                "frontend", "backend", "fullstack", "mobile", "ios", "android",
                "flutter", "react-native", "git", "github", "gitlab", "agile",
                "scrum", "kanban", "linux", "bash", "shell", "vim", "vscode",
                "intellij", "debugging", "monitoring", "logging", "prometheus",
                "grafana", "jenkins", "terraform", "ansible", "nginx", "apache",
                "cache", "cdn", "load-balancing", "scalability", "distributed-system",
                "event-driven", "domain-driven-design", "solid", "tdd", "bdd",
                "refactoring", "code-review", "documentation", "api-design", "ux",
            )
    }

    private val processedCount = AtomicLong(0)
    private val processedTagsCount = AtomicLong(0)
    private val startTime = AtomicLong(0)
    private val tagIdMap = ConcurrentHashMap<String, Long>()

    fun loadPosts(totalCount: Long = 20_000_000) {
        logger.info("========================================")
        logger.info("대량 게시글 및 태그 데이터 삽입 시작")
        logger.info("목표 게시글: ${String.format("%,d", totalCount)} 건")
        logger.info("태그 종류: ${TAG_NAMES.size} 개")
        logger.info("배치 크기: ${String.format("%,d", BATCH_SIZE)} 건")
        logger.info("병렬 워커: $PARALLEL_WORKERS 개")
        logger.info("========================================")

        startTime.set(System.currentTimeMillis())
        processedCount.set(0)
        processedTagsCount.set(0)

        try {
            // 1. 태그 마스터 데이터 초기화
            initializeTags()

            // 2. 게시글 및 게시글-태그 연결 데이터 삽입
            runBlocking(Dispatchers.IO) {
                val chunksPerWorker = totalCount / PARALLEL_WORKERS
                val jobs =
                    (0 until PARALLEL_WORKERS).map { workerId ->
                        async {
                            val startIdx = workerId * chunksPerWorker
                            val endIdx =
                                if (workerId == PARALLEL_WORKERS - 1) {
                                    totalCount
                                } else {
                                    (workerId + 1) * chunksPerWorker
                                }
                            processChunk(startIdx, endIdx, workerId, totalCount)
                        }
                    }
                jobs.awaitAll()
            }

            // 3. 태그 post_count 업데이트
            updateTagPostCounts()

            printFinalSummary()
        } catch (e: Exception) {
            logger.error("데이터 삽입 중 오류 발생", e)
            throw e
        }
    }

    private fun initializeTags() {
        logger.info("태그 마스터 데이터 초기화 중...")

        val now = Instant.now()
        val sql =
            """
            INSERT IGNORE INTO tags (id, name, post_count, created_at, updated_at) 
            VALUES (?, ?, 0, ?, ?)
            """.trimIndent()

        jdbcTemplate.batchUpdate(
            sql,
            TAG_NAMES.map { tagName ->
                val tagId = snowflake.nextId()
                tagIdMap[tagName] = tagId
                arrayOf(tagId, tagName, Timestamp.from(now), Timestamp.from(now))
            },
        )

        logger.info("${TAG_NAMES.size}개 태그 초기화 완료")
    }

    private fun updateTagPostCounts() {
        logger.info("태그 post_count 업데이트 중...")

        // 방법 1: JOIN을 사용한 더 효율적인 업데이트
        // 임시 테이블을 사용하여 집계 결과를 먼저 계산
        val createTempTableSql =
            """
            CREATE TEMPORARY TABLE IF NOT EXISTS temp_tag_counts AS
            SELECT tag_id, COUNT(DISTINCT post_id) as count
            FROM post_tags
            GROUP BY tag_id
            """.trimIndent()

        val updateSql =
            """
            UPDATE tags t
            INNER JOIN temp_tag_counts tc ON t.id = tc.tag_id
            SET t.post_count = tc.count
            """.trimIndent()

        val dropTempTableSql = "DROP TEMPORARY TABLE IF EXISTS temp_tag_counts"

        try {
            // 임시 테이블 생성 및 데이터 집계
            jdbcTemplate.execute(createTempTableSql)
            logger.info("태그별 게시글 수 집계 완료")

            // 배치 업데이트 실행
            val updatedCount = jdbcTemplate.update(updateSql)
            logger.info("$updatedCount 개 태그의 post_count 업데이트 완료")
        } catch (e: Exception) {
            logger.warn("임시 테이블 방식 실패, 배치 업데이트로 재시도: ${e.message}")
            updateTagPostCountsInBatches()
        } finally {
            // 임시 테이블 정리
            try {
                jdbcTemplate.execute(dropTempTableSql)
            } catch (e: Exception) {
                // 임시 테이블 삭제 실패는 무시
            }
        }
    }

    private fun updateTagPostCountsInBatches() {
        logger.info("배치 방식으로 태그 post_count 업데이트 중...")

        // 태그를 작은 배치로 나누어 업데이트
        val batchSize = 10
        val tagIds = tagIdMap.values.toList()

        for (i in tagIds.indices step batchSize) {
            val batch = tagIds.subList(i, minOf(i + batchSize, tagIds.size))
            val idList = batch.joinToString(",")

            val sql =
                """
                UPDATE tags t
                SET post_count = (
                    SELECT COUNT(DISTINCT pt.post_id)
                    FROM post_tags pt
                    WHERE pt.tag_id = t.id
                )
                WHERE t.id IN ($idList)
                """.trimIndent()

            jdbcTemplate.execute(sql)

            if ((i + batchSize) % 50 == 0) {
                logger.info("${minOf(i + batchSize, tagIds.size)} / ${tagIds.size} 태그 업데이트 완료")
            }
        }

        logger.info("모든 태그 post_count 업데이트 완료")
    }

    private suspend fun processChunk(
        startIdx: Long,
        endIdx: Long,
        workerId: Int,
        totalCount: Long,
    ) {
        var connection: Connection? = null
        var postStatement: PreparedStatement? = null
        var tagStatement: PreparedStatement? = null

        try {
            connection = dataSource.connection
            connection.autoCommit = false

            val postSql =
                """
                INSERT INTO posts (id, title, body, status, version, created_at, updated_at) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

            val tagSql =
                """
                INSERT INTO post_tags (post_id, tag_id, display_order, created_at) 
                VALUES (?, ?, ?, ?)
                """.trimIndent()

            postStatement = connection.prepareStatement(postSql)
            tagStatement = connection.prepareStatement(tagSql)

            val postBatch = mutableListOf<Long>()
            val random = ThreadLocalRandom.current()
            val oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS)
            val tagNamesList = TAG_NAMES.toList()

            for (i in startIdx until endIdx) {
                val postId = snowflake.nextId()
                val titleTemplate = TITLE_TEMPLATES[random.nextInt(TITLE_TEMPLATES.size)]
                val title = String.format(titleTemplate, i)
                val body =
                    BODY_TEMPLATES[random.nextInt(BODY_TEMPLATES.size)] +
                        "\n\n게시글 번호: $i\n워커 ID: $workerId"
                val status = selectRandomStatus(random)
                val version = 0L
                val createdAt =
                    Instant.ofEpochMilli(
                        random.nextLong(oneYearAgo.toEpochMilli(), Instant.now().toEpochMilli()),
                    )
                val updatedAt = createdAt.plus(random.nextLong(0, 30), ChronoUnit.DAYS)

                // 게시글 삽입
                postStatement.setLong(1, postId)
                postStatement.setString(2, title)
                postStatement.setString(3, body)
                postStatement.setString(4, status)
                postStatement.setLong(5, version)
                postStatement.setTimestamp(6, Timestamp.from(createdAt))
                postStatement.setTimestamp(7, Timestamp.from(updatedAt))
                postStatement.addBatch()

                // 게시글에 랜덤 태그 추가 (0-5개)
                val tagCount = random.nextInt(MAX_TAGS_PER_POST + 1)
                if (tagCount > 0) {
                    val selectedTags = mutableSetOf<String>()
                    while (selectedTags.size < tagCount) {
                        selectedTags.add(tagNamesList[random.nextInt(tagNamesList.size)])
                    }

                    selectedTags.forEachIndexed { index, tagName ->
                        val tagId = tagIdMap[tagName] ?: return@forEachIndexed
                        tagStatement.setLong(1, postId)
                        tagStatement.setLong(2, tagId)
                        tagStatement.setInt(3, index)
                        tagStatement.setTimestamp(4, Timestamp.from(createdAt))
                        tagStatement.addBatch()
                    }

                    processedTagsCount.addAndGet(selectedTags.size.toLong())
                }

                postBatch.add(postId)

                // 배치 실행
                if (postBatch.size >= BATCH_SIZE) {
                    postStatement.executeBatch()
                    tagStatement.executeBatch()
                    connection.commit()

                    val currentProcessed = processedCount.addAndGet(postBatch.size.toLong())
                    if (currentProcessed % PROGRESS_INTERVAL == 0L) {
                        printProgress(currentProcessed, totalCount)
                    }

                    postBatch.clear()
                }
            }

            // 남은 배치 처리
            if (postBatch.isNotEmpty()) {
                postStatement.executeBatch()
                tagStatement.executeBatch()
                connection.commit()
                processedCount.addAndGet(postBatch.size.toLong())
            }
        } catch (e: Exception) {
            logger.error("워커 $workerId 처리 중 오류 발생", e)
            connection?.rollback()
            throw e
        } finally {
            postStatement?.close()
            tagStatement?.close()
            connection?.close()
        }
    }

    private fun selectRandomStatus(random: ThreadLocalRandom): String {
        val value = random.nextInt(100)
        var accumulated = 0

        for ((status, weight) in POST_STATUSES) {
            accumulated += weight
            if (value < accumulated) {
                return status
            }
        }

        return "DRAFT"
    }

    private fun printProgress(
        currentCount: Long,
        totalCount: Long,
    ) {
        val elapsedMs = System.currentTimeMillis() - startTime.get()
        val elapsedSeconds = elapsedMs / 1000.0
        val throughput = if (elapsedSeconds > 0) currentCount / elapsedSeconds else 0.0
        val progressPercentage = (currentCount.toDouble() / totalCount) * 100
        val estimatedTotalSeconds = if (throughput > 0) totalCount / throughput else 0.0
        val remainingSeconds = estimatedTotalSeconds - elapsedSeconds

        logger.info("")
        logger.info("===== 대량 데이터 삽입 진행 현황 =====")
        logger.info(
            "처리 완료: ${String.format(
                "%,d",
                currentCount,
            )} / ${String.format("%,d", totalCount)} 건 (${String.format("%.1f", progressPercentage)}%)",
        )
        logger.info("소요 시간: ${formatDuration(elapsedSeconds.toLong())}")
        logger.info("처리 속도: ${String.format("%,.0f", throughput)} 건/초")
        if (remainingSeconds > 0) {
            logger.info("예상 남은 시간: ${formatDuration(remainingSeconds.toLong())}")
        }
        logger.info("=====================================")
        logger.info("")
    }

    private fun printFinalSummary() {
        val finalCount = processedCount.get()
        val finalTagsCount = processedTagsCount.get()
        val totalElapsedMs = System.currentTimeMillis() - startTime.get()
        val totalElapsedSeconds = totalElapsedMs / 1000.0
        val avgThroughput = if (totalElapsedSeconds > 0) finalCount / totalElapsedSeconds else 0.0
        val avgTagsPerPost = if (finalCount > 0) finalTagsCount.toDouble() / finalCount else 0.0

        logger.info("")
        logger.info("========================================")
        logger.info("대량 데이터 삽입 완료!")
        logger.info("========================================")
        logger.info("총 게시글 수: ${String.format("%,d", finalCount)} 건")
        logger.info("총 태그 연결 수: ${String.format("%,d", finalTagsCount)} 건")
        logger.info("게시글당 평균 태그: ${String.format("%.1f", avgTagsPerPost)} 개")
        logger.info("총 소요 시간: ${formatDuration(totalElapsedSeconds.toLong())}")
        logger.info("평균 처리 속도: ${String.format("%,.0f", avgThroughput)} 건/초")
        logger.info("========================================")
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> "${hours}시간 ${minutes}분 ${secs}초"
            minutes > 0 -> "${minutes}분 ${secs}초"
            else -> "${secs}초"
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun clearAllPosts() {
        logger.warn("모든 게시글 및 태그 데이터를 삭제합니다...")

        // 연결 테이블 먼저 삭제
        jdbcTemplate.execute("DELETE FROM post_tags")
        logger.info("게시글-태그 연결 데이터 삭제 완료")

        // 게시글 삭제
        jdbcTemplate.execute("DELETE FROM posts")
        logger.info("게시글 데이터 삭제 완료")

        // 태그 삭제 (선택적 - 태그는 유지하고 싶을 수도 있음)
        // jdbcTemplate.execute("DELETE FROM tags")
        // logger.info("태그 마스터 데이터 삭제 완료")
    }

    fun getPostCount(): Long {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM posts", Long::class.java) ?: 0L
    }

    fun getTagCount(): Long {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tags", Long::class.java) ?: 0L
    }

    fun getPostTagCount(): Long {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post_tags", Long::class.java) ?: 0L
    }
}
