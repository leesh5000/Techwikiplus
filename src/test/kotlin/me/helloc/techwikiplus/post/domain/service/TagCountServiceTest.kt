package me.helloc.techwikiplus.post.domain.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.common.infrastructure.FakeTagJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.TagEntity
import me.helloc.techwikiplus.post.domain.model.tag.Tag
import me.helloc.techwikiplus.post.domain.model.tag.TagId
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TagCountServiceTest : DescribeSpec({
    var executorService: ExecutorService? = null

    afterEach {
        // ExecutorService가 있다면 정리
        executorService?.let { executor ->
            executor.shutdown()
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow()
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        println("ExecutorService did not terminate")
                    }
                }
            } catch (e: InterruptedException) {
                executor.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
        executorService = null
    }

    fun createTagCountService(): Pair<TagCountService, FakeTagJpaRepository> {
        val tagJpaRepository = FakeTagJpaRepository()
        val tagCountService = TagCountService(tagJpaRepository)
        return tagCountService to tagJpaRepository
    }

    fun createTestTag(
        id: Long,
        name: String,
        postCount: Int = 0,
    ): Tag {
        val now = Instant.now()
        return Tag(
            id = TagId(id),
            name = TagName(name),
            postCount = postCount,
            createdAt = now,
            updatedAt = now,
        )
    }

    fun setupTagInRepository(
        repository: FakeTagJpaRepository,
        id: Long,
        name: String,
        postCount: Int = 0,
    ) {
        val tagEntity =
            TagEntity(
                id = id,
                name = name,
                postCount = postCount,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        repository.addTag(tagEntity)
    }

    describe("incrementPostCount") {
        context("빈 태그 리스트가 주어졌을 때") {
            it("아무 동작도 하지 않는다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                val emptyTags = emptyList<Tag>()

                // when
                tagCountService.incrementPostCount(emptyTags)

                // then
                repository.count() shouldBe 0
            }
        }

        context("단일 태그가 주어졌을 때") {
            it("해당 태그의 카운트를 1 증가시킨다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                setupTagInRepository(repository, 1L, "spring", 5)
                val tag = createTestTag(1L, "spring", 5)

                // when
                tagCountService.incrementPostCount(listOf(tag))

                // then
                repository.getPostCount(1L) shouldBe 6
            }
        }

        context("여러 태그가 주어졌을 때") {
            it("모든 태그의 카운트를 각각 1씩 증가시킨다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                setupTagInRepository(repository, 1L, "spring", 10)
                setupTagInRepository(repository, 2L, "kotlin", 5)
                setupTagInRepository(repository, 3L, "java", 3)

                val tags =
                    listOf(
                        createTestTag(1L, "spring", 10),
                        createTestTag(2L, "kotlin", 5),
                        createTestTag(3L, "java", 3),
                    )

                // when
                tagCountService.incrementPostCount(tags)

                // then
                repository.getPostCount(1L) shouldBe 11
                repository.getPostCount(2L) shouldBe 6
                repository.getPostCount(3L) shouldBe 4
            }
        }

        context("같은 태그를 여러 번 증가시킬 때") {
            it("호출한 횟수만큼 카운트가 증가한다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                setupTagInRepository(repository, 1L, "spring", 0)
                val tag = createTestTag(1L, "spring", 0)

                // when
                repeat(5) {
                    tagCountService.incrementPostCount(listOf(tag))
                }

                // then
                repository.getPostCount(1L) shouldBe 5
            }
        }

        context("동시에 여러 스레드에서 증가시킬 때") {
            it("모든 증가 연산이 안전하게 처리된다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                setupTagInRepository(repository, 1L, "spring", 0)
                val tag = createTestTag(1L, "spring", 0)

                val threadCount = 10
                val incrementsPerThread = 100
                val latch = CountDownLatch(threadCount)
                executorService = Executors.newFixedThreadPool(threadCount)

                // when
                repeat(threadCount) {
                    executorService?.submit {
                        try {
                            repeat(incrementsPerThread) {
                                tagCountService.incrementPostCount(listOf(tag))
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await(10, TimeUnit.SECONDS)

                // then
                repository.getPostCount(1L) shouldBe (threadCount * incrementsPerThread)
            }
        }
    }

    describe("decrementPostCount") {
        context("빈 태그 리스트가 주어졌을 때") {
            it("아무 동작도 하지 않는다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                val emptyTags = emptyList<Tag>()

                // when
                tagCountService.decrementPostCount(emptyTags)

                // then
                repository.count() shouldBe 0
            }
        }

        context("단일 태그가 주어졌을 때") {
            it("해당 태그의 카운트를 1 감소시킨다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                setupTagInRepository(repository, 1L, "spring", 5)
                val tag = createTestTag(1L, "spring", 5)

                // when
                tagCountService.decrementPostCount(listOf(tag))

                // then
                repository.getPostCount(1L) shouldBe 4
            }
        }

        context("여러 태그가 주어졌을 때") {
            it("모든 태그의 카운트를 각각 1씩 감소시킨다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                setupTagInRepository(repository, 1L, "spring", 10)
                setupTagInRepository(repository, 2L, "kotlin", 5)
                setupTagInRepository(repository, 3L, "java", 3)

                val tags =
                    listOf(
                        createTestTag(1L, "spring", 10),
                        createTestTag(2L, "kotlin", 5),
                        createTestTag(3L, "java", 3),
                    )

                // when
                tagCountService.decrementPostCount(tags)

                // then
                repository.getPostCount(1L) shouldBe 9
                repository.getPostCount(2L) shouldBe 4
                repository.getPostCount(3L) shouldBe 2
            }
        }

        context("카운트가 0인 태그를 감소시킬 때") {
            it("카운트가 0 이하로 내려가지 않는다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                setupTagInRepository(repository, 1L, "spring", 0)
                val tag = createTestTag(1L, "spring", 0)

                // when
                tagCountService.decrementPostCount(listOf(tag))

                // then
                repository.getPostCount(1L) shouldBe 0
            }
        }

        context("카운트가 1인 태그를 여러 번 감소시킬 때") {
            it("카운트가 0에서 멈춘다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                setupTagInRepository(repository, 1L, "spring", 1)
                val tag = createTestTag(1L, "spring", 1)

                // when
                repeat(5) {
                    tagCountService.decrementPostCount(listOf(tag))
                }

                // then
                repository.getPostCount(1L) shouldBe 0
            }
        }

        context("동시에 여러 스레드에서 감소시킬 때") {
            it("모든 감소 연산이 안전하게 처리되고 0 이하로 내려가지 않는다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                val initialCount = 50
                setupTagInRepository(repository, 1L, "spring", initialCount)
                val tag = createTestTag(1L, "spring", initialCount)

                val threadCount = 10
                val decrementsPerThread = 10
                val latch = CountDownLatch(threadCount)
                executorService = Executors.newFixedThreadPool(threadCount)

                // when
                repeat(threadCount) {
                    executorService?.submit {
                        try {
                            repeat(decrementsPerThread) {
                                tagCountService.decrementPostCount(listOf(tag))
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await(10, TimeUnit.SECONDS)

                // then
                // 50 - (10 * 10) = -50이 되어야 하지만, 0에서 멈춘다
                repository.getPostCount(1L) shouldBe 0
            }
        }

        context("동시에 증가와 감소가 혼합될 때") {
            it("모든 연산이 원자적으로 처리된다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                val initialCount = 100
                setupTagInRepository(repository, 1L, "spring", initialCount)
                val tag = createTestTag(1L, "spring", initialCount)

                val threadCount = 20
                val operationsPerThread = 50
                val latch = CountDownLatch(threadCount)
                executorService = Executors.newFixedThreadPool(threadCount)

                // when
                // 절반은 증가, 절반은 감소
                repeat(threadCount) { threadIndex ->
                    executorService?.submit {
                        try {
                            repeat(operationsPerThread) {
                                if (threadIndex < threadCount / 2) {
                                    tagCountService.incrementPostCount(listOf(tag))
                                } else {
                                    tagCountService.decrementPostCount(listOf(tag))
                                }
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await(10, TimeUnit.SECONDS)

                // then
                // 초기값 100 + (10 * 50) - (10 * 50) = 100
                repository.getPostCount(1L) shouldBe 100
            }
        }
    }

    describe("incrementPostCount와 decrementPostCount 통합") {
        context("증가 후 같은 수만큼 감소시킬 때") {
            it("원래 값으로 돌아온다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                val initialCount = 5
                setupTagInRepository(repository, 1L, "spring", initialCount)
                setupTagInRepository(repository, 2L, "kotlin", initialCount)

                val tags =
                    listOf(
                        createTestTag(1L, "spring", initialCount),
                        createTestTag(2L, "kotlin", initialCount),
                    )

                // when
                repeat(3) {
                    tagCountService.incrementPostCount(tags)
                }
                repeat(3) {
                    tagCountService.decrementPostCount(tags)
                }

                // then
                repository.getPostCount(1L) shouldBe initialCount
                repository.getPostCount(2L) shouldBe initialCount
            }
        }

        context("서로 다른 태그 그룹을 처리할 때") {
            it("각 태그가 독립적으로 카운트된다") {
                // given
                val (tagCountService, repository) = createTagCountService()
                setupTagInRepository(repository, 1L, "spring", 0)
                setupTagInRepository(repository, 2L, "kotlin", 0)
                setupTagInRepository(repository, 3L, "java", 0)

                val group1 =
                    listOf(
                        createTestTag(1L, "spring", 0),
                        createTestTag(2L, "kotlin", 0),
                    )
                val group2 =
                    listOf(
                        createTestTag(2L, "kotlin", 0),
                        createTestTag(3L, "java", 0),
                    )

                // when
                tagCountService.incrementPostCount(group1) // spring: 1, kotlin: 1
                tagCountService.incrementPostCount(group2) // kotlin: 2, java: 1
                tagCountService.decrementPostCount(group1) // spring: 0, kotlin: 1

                // then
                repository.getPostCount(1L) shouldBe 0 // spring
                repository.getPostCount(2L) shouldBe 1 // kotlin
                repository.getPostCount(3L) shouldBe 1 // java
            }
        }
    }
})
