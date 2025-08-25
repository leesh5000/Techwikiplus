package me.helloc.techwikiplus.post.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.common.infrastructure.FakeClockHolder
import me.helloc.techwikiplus.common.infrastructure.FakeTagIdGenerator
import me.helloc.techwikiplus.common.infrastructure.FakeTagRepository
import me.helloc.techwikiplus.common.infrastructure.NoOpLockManager
import me.helloc.techwikiplus.common.infrastructure.SlowLockManager
import me.helloc.techwikiplus.common.infrastructure.TestLockManager
import me.helloc.techwikiplus.common.infrastructure.TrackingLockManager
import me.helloc.techwikiplus.post.domain.model.tag.Tag
import me.helloc.techwikiplus.post.domain.model.tag.TagId
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.user.domain.service.port.LockManagerException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TagServiceTest : DescribeSpec({

    /**
     * 테스트용 TagService 생성 헬퍼 메서드
     * FIRST 원칙의 Independent를 위해 매번 새로운 인스턴스 생성
     */
    fun createTagService(
        repository: FakeTagRepository = FakeTagRepository(),
        idGenerator: FakeTagIdGenerator = FakeTagIdGenerator(startId = 100L),
        clockTime: Instant = Instant.parse("2025-01-25T10:00:00Z"),
        lockManager: me.helloc.techwikiplus.user.domain.service.port.LockManager = NoOpLockManager(),
    ): TagService {
        return TagService(
            tagRepository = repository,
            tagIdGenerator = idGenerator,
            clockHolder = FakeClockHolder(clockTime),
            lockManager = lockManager,
        )
    }

    // 동시성 테스트에서 사용하는 ExecutorService를 추적
    var executorService: ExecutorService? = null

    afterEach {
        // 각 테스트 후 ExecutorService 정리
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

    describe("TagService") {

        describe("findByName - 단일 태그명으로 조회") {

            it("존재하는 태그를 조회한다") {
                // Given: 저장소에 태그가 존재
                val repository = FakeTagRepository()
                val existingTag =
                    Tag.create(
                        id = TagId(1L),
                        name = TagName("spring"),
                        now = Instant.parse("2025-01-25T10:00:00Z"),
                    )
                repository.save(existingTag)

                val tagService = createTagService(repository = repository)

                // When: 태그명으로 조회
                val result = tagService.findByName(TagName("spring"))

                // Then: 태그가 조회됨
                result.shouldNotBeNull()
                result.id shouldBe TagId(1L)
                result.name shouldBe TagName("spring")
            }

            it("존재하지 않는 태그는 null을 반환한다") {
                // Given: 빈 저장소
                val repository = FakeTagRepository()
                val tagService = createTagService(repository = repository)

                // When: 존재하지 않는 태그 조회
                val result = tagService.findByName(TagName("nonexistent"))

                // Then: null 반환
                result.shouldBeNull()
            }
        }

        describe("findByNames - 여러 태그명으로 조회") {

            it("모든 태그가 존재하는 경우 모두 반환한다") {
                // Given: 저장소에 여러 태그 존재
                val repository = FakeTagRepository()
                val tag1 = Tag.create(TagId(1L), TagName("spring"), Instant.now())
                val tag2 = Tag.create(TagId(2L), TagName("kotlin"), Instant.now())
                val tag3 = Tag.create(TagId(3L), TagName("java"), Instant.now())
                repository.save(tag1)
                repository.save(tag2)
                repository.save(tag3)

                val tagService = createTagService(repository = repository)

                // When: 여러 태그명으로 조회
                val result =
                    tagService.findByNames(
                        listOf(TagName("spring"), TagName("kotlin"), TagName("java")),
                    )

                // Then: 모든 태그가 반환됨
                result shouldHaveSize 3
                result.map { it.name } shouldContainExactly
                    listOf(
                        TagName("spring"),
                        TagName("kotlin"),
                        TagName("java"),
                    )
            }

            it("일부만 존재하는 경우 존재하는 태그만 반환한다") {
                // Given: 저장소에 일부 태그만 존재
                val repository = FakeTagRepository()
                val tag1 = Tag.create(TagId(1L), TagName("spring"), Instant.now())
                repository.save(tag1)

                val tagService = createTagService(repository = repository)

                // When: 여러 태그명으로 조회
                val result =
                    tagService.findByNames(
                        listOf(TagName("spring"), TagName("kotlin"), TagName("java")),
                    )

                // Then: 존재하는 태그만 반환
                result shouldHaveSize 1
                result[0].name shouldBe TagName("spring")
            }

            it("빈 리스트로 조회하면 빈 리스트를 반환한다") {
                // Given: 태그가 있는 저장소
                val repository = FakeTagRepository()
                val tagService = createTagService(repository = repository)

                // When: 빈 리스트로 조회
                val result = tagService.findByNames(emptyList())

                // Then: 빈 리스트 반환
                result.shouldBeEmpty()
            }

            it("모든 태그가 존재하지 않으면 빈 리스트를 반환한다") {
                // Given: 빈 저장소
                val repository = FakeTagRepository()
                val tagService = createTagService(repository = repository)

                // When: 존재하지 않는 태그들로 조회
                val result =
                    tagService.findByNames(
                        listOf(TagName("nonexistent1"), TagName("nonexistent2")),
                    )

                // Then: 빈 리스트 반환
                result.shouldBeEmpty()
            }
        }

        describe("findOrCreateTags - 태그 조회 또는 생성") {

            it("모든 태그가 새로 생성되는 경우") {
                // Given: 빈 저장소와 TagService
                val repository = FakeTagRepository()
                val idGenerator = FakeTagIdGenerator(startId = 100L)
                val clockHolder = FakeClockHolder(Instant.parse("2025-01-25T10:00:00Z"))
                val tagService =
                    createTagService(
                        repository = repository,
                        idGenerator = idGenerator,
                        clockTime = clockHolder.now(),
                    )

                // When: 새 태그들 생성
                val tagNames = listOf(TagName("spring"), TagName("kotlin"), TagName("java"))
                val result = tagService.findOrCreateTags(tagNames)

                // Then: 모든 태그가 생성됨
                result shouldHaveSize 3
                result[0].id shouldBe TagId(100L)
                result[0].name shouldBe TagName("spring")
                result[1].id shouldBe TagId(101L)
                result[1].name shouldBe TagName("kotlin")
                result[2].id shouldBe TagId(102L)
                result[2].name shouldBe TagName("java")

                // 저장소에 저장되었는지 확인
                repository.count() shouldBe 3
            }

            it("모든 태그가 이미 존재하는 경우 기존 태그를 반환한다") {
                // Given: 태그들이 이미 존재하는 저장소
                val repository = FakeTagRepository()
                val existingTags =
                    listOf(
                        Tag.create(TagId(1L), TagName("spring"), Instant.now()),
                        Tag.create(TagId(2L), TagName("kotlin"), Instant.now()),
                        Tag.create(TagId(3L), TagName("java"), Instant.now()),
                    )
                existingTags.forEach { repository.save(it) }

                val tagService = createTagService(repository = repository)

                // When: 기존 태그들 조회
                val tagNames = listOf(TagName("spring"), TagName("kotlin"), TagName("java"))
                val result = tagService.findOrCreateTags(tagNames)

                // Then: 기존 태그들이 반환됨
                result shouldHaveSize 3
                result[0].id shouldBe TagId(1L)
                result[1].id shouldBe TagId(2L)
                result[2].id shouldBe TagId(3L)

                // 추가 생성 없음
                repository.count() shouldBe 3
            }

            it("일부는 존재하고 일부는 새로 생성한다") {
                // Given: 일부 태그만 존재하는 저장소
                val repository = FakeTagRepository()
                val existingTag =
                    Tag.create(
                        id = TagId(200L),
                        name = TagName("elasticsearch"),
                        now = Instant.now(),
                    )
                repository.save(existingTag)

                val idGenerator = FakeTagIdGenerator(startId = 300L)
                val tagService =
                    createTagService(
                        repository = repository,
                        idGenerator = idGenerator,
                    )

                // When: 일부 존재, 일부 새로 생성
                val tagNames =
                    listOf(
                        // 새로 생성 (ID: 300)
                        TagName("postgresql"),
                        // 기존 존재 (ID: 200)
                        TagName("elasticsearch"),
                        // 새로 생성 (ID: 301)
                        TagName("mongodb"),
                    )
                val result = tagService.findOrCreateTags(tagNames)

                // Then: 입력 순서대로 반환, ID는 처리 순서에 따라 할당
                result shouldHaveSize 3
                result[0].name shouldBe TagName("postgresql")
                // 새로 생성
                result[0].id shouldBe TagId(300L)
                result[1].name shouldBe TagName("elasticsearch")
                // 기존 태그
                result[1].id shouldBe TagId(200L)
                result[2].name shouldBe TagName("mongodb")
                // 새로 생성
                result[2].id shouldBe TagId(301L)

                repository.count() shouldBe 3
            }

            it("빈 리스트를 입력하면 빈 리스트를 반환한다") {
                // Given: TagService
                val repository = FakeTagRepository()
                val tagService = createTagService(repository = repository)

                // When: 빈 리스트로 호출
                val result = tagService.findOrCreateTags(emptyList())

                // Then: 빈 리스트 반환
                result.shouldBeEmpty()
                repository.count() shouldBe 0
            }

            it("중복된 태그명이 입력되면 중복을 제거하고 처리한다") {
                // Given: 빈 저장소
                val repository = FakeTagRepository()
                val idGenerator = FakeTagIdGenerator(startId = 400L)
                val tagService =
                    createTagService(
                        repository = repository,
                        idGenerator = idGenerator,
                    )

                // When: 중복된 태그명 입력
                val tagNames =
                    listOf(
                        TagName("spring"),
                        TagName("kotlin"),
                        // 중복
                        TagName("spring"),
                        TagName("java"),
                        // 중복
                        TagName("kotlin"),
                    )
                val result = tagService.findOrCreateTags(tagNames)

                // Then: 중복 제거되어 3개만 생성
                result shouldHaveSize 3
                result[0].name shouldBe TagName("spring")
                result[0].id shouldBe TagId(400L)
                result[1].name shouldBe TagName("kotlin")
                result[1].id shouldBe TagId(401L)
                result[2].name shouldBe TagName("java")
                result[2].id shouldBe TagId(402L)

                repository.count() shouldBe 3
            }

            it("입력 순서대로 태그를 처리하고 반환한다") {
                // Given: 빈 저장소
                val repository = FakeTagRepository()
                val idGenerator = FakeTagIdGenerator(startId = 500L)
                val tagService =
                    createTagService(
                        repository = repository,
                        idGenerator = idGenerator,
                    )

                // When: 특정 순서로 태그 입력
                val tagNames =
                    listOf(
                        TagName("zebra"),
                        TagName("apple"),
                        TagName("mango"),
                        TagName("banana"),
                    )
                val result = tagService.findOrCreateTags(tagNames)

                // Then: 입력 순서대로 처리 및 반환
                result shouldHaveSize 4
                result[0].name shouldBe TagName("zebra")
                result[0].id shouldBe TagId(500L)
                result[1].name shouldBe TagName("apple")
                result[1].id shouldBe TagId(501L)
                result[2].name shouldBe TagName("mango")
                result[2].id shouldBe TagId(502L)
                result[3].name shouldBe TagName("banana")
                result[3].id shouldBe TagId(503L)
            }
        }

        describe("동시성 제어") {

            it("동일 태그에 대한 동시 생성 요청을 안전하게 처리한다") {
                // Given: 동시성을 시뮬레이션하는 환경
                val repository = FakeTagRepository()
                val idGenerator = FakeTagIdGenerator(startId = 1000L)
                val lockManager = TestLockManager() // synchronized 기반 락
                val tagService =
                    createTagService(
                        repository = repository,
                        idGenerator = idGenerator,
                        lockManager = lockManager,
                    )

                val threadCount = 10
                val latch = CountDownLatch(threadCount)
                val createdTags = mutableListOf<Tag>()
                val errors = mutableListOf<Exception>()

                executorService = Executors.newFixedThreadPool(threadCount)

                // When: 여러 스레드에서 동시에 같은 태그 생성 시도
                repeat(threadCount) {
                    executorService!!.execute {
                        try {
                            val result = tagService.findOrCreateTags(listOf(TagName("concurrent")))
                            synchronized(createdTags) {
                                createdTags.addAll(result)
                            }
                        } catch (e: Exception) {
                            synchronized(errors) {
                                errors.add(e)
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await(10, TimeUnit.SECONDS)

                // Then: 하나의 태그만 생성되고, 모든 스레드가 같은 태그를 받음
                errors shouldHaveSize 0
                createdTags shouldHaveSize threadCount
                createdTags.map { it.id }.toSet() shouldHaveSize 1 // 모두 같은 ID
                createdTags.map { it.name }.toSet() shouldHaveSize 1 // 모두 같은 이름
                repository.count() shouldBe 1 // 저장소에는 하나만 존재
            }

            it("서로 다른 태그에 대한 동시 생성은 병렬 처리된다") {
                // Given: 동시성 환경
                val repository = FakeTagRepository()
                val idGenerator = FakeTagIdGenerator(startId = 2000L)
                val lockManager = TestLockManager()
                val tagService =
                    createTagService(
                        repository = repository,
                        idGenerator = idGenerator,
                        lockManager = lockManager,
                    )

                val tagNames = listOf("tag1", "tag2", "tag3", "tag4", "tag5")
                val latch = CountDownLatch(tagNames.size)
                val createdTags = mutableListOf<Tag>()

                executorService = Executors.newFixedThreadPool(tagNames.size)

                // When: 서로 다른 태그를 동시에 생성
                tagNames.forEach { tagName ->
                    executorService!!.execute {
                        val result = tagService.findOrCreateTags(listOf(TagName(tagName)))
                        synchronized(createdTags) {
                            createdTags.addAll(result)
                        }
                        latch.countDown()
                    }
                }

                latch.await(10, TimeUnit.SECONDS)

                // Then: 모든 태그가 생성됨
                createdTags shouldHaveSize tagNames.size
                createdTags.map { it.name.value }.toSet() shouldHaveSize tagNames.size
                repository.count() shouldBe tagNames.size
            }

            it("입력 순서대로 락을 획득하여 순차 처리한다") {
                // Given: 락 사용을 추적하는 환경
                val repository = FakeTagRepository()
                repository.clear() // 명시적으로 저장소 초기화

                val idGenerator = FakeTagIdGenerator(startId = 3000L)
                val lockManager = TrackingLockManager()
                lockManager.resetCounts() // 락 카운트 초기화

                val tagService =
                    createTagService(
                        repository = repository,
                        idGenerator = idGenerator,
                        lockManager = lockManager,
                    )

                // 태그가 존재하지 않는지 사전 확인
                val tagNames =
                    listOf(
                        TagName("zebra"),
                        TagName("apple"),
                        TagName("mango"),
                    )
                tagNames.forEach { tagName ->
                    repository.findBy(tagName) shouldBe null
                }

                // When: 입력 순서대로 태그 생성
                val result = tagService.findOrCreateTags(tagNames)

                // Then: 입력 순서대로 처리됨
                result shouldHaveSize 3
                result[0].name shouldBe TagName("zebra")
                result[1].name shouldBe TagName("apple")
                result[2].name shouldBe TagName("mango")

                // 각 태그에 대해 락이 한 번씩만 획득됨
                lockManager.getLockCount("tag:create:zebra") shouldBe 1
                lockManager.getLockCount("tag:create:apple") shouldBe 1
                lockManager.getLockCount("tag:create:mango") shouldBe 1
            }

            it("락 획득 실패 시 예외가 발생한다") {
                // Given: 항상 실패하는 락 매니저
                val repository = FakeTagRepository()
                val lockManager = SlowLockManager()
                val tagService =
                    createTagService(
                        repository = repository,
                        lockManager = lockManager,
                    )

                // When & Then: 락 획득 실패로 예외 발생
                shouldThrow<LockManagerException> {
                    tagService.findOrCreateTags(listOf(TagName("failedlock")))
                }
            }

            it("DataIntegrityViolationException 발생 시 재조회한다") {
                // Given: Double-check 시나리오를 시뮬레이션하는 환경
                val repository = FakeTagRepository()
                val idGenerator = FakeTagIdGenerator(startId = 4000L)
                val clockHolder = FakeClockHolder(Instant.now())

                // 미리 생성될 태그 (다른 스레드가 생성한 것으로 시뮬레이션)
                val duplicateTag =
                    Tag.create(
                        id = TagId(4000L),
                        name = TagName("duplicate"),
                        now = clockHolder.now(),
                    )

                // DataIntegrityViolationException을 시뮬레이션하는 커스텀 락 매니저
                var callCount = 0
                val customLockManager =
                    object : me.helloc.techwikiplus.user.domain.service.port.LockManager {
                        override fun <T> executeWithLock(
                            key: String,
                            waitTime: Duration,
                            leaseTime: Duration,
                            block: () -> T,
                        ): T {
                            callCount++
                            if (callCount == 1) {
                                // 첫 번째 호출: 태그가 없는 것처럼 보이다가
                                // save 시점에 다른 스레드가 이미 생성했다고 시뮬레이션

                                // 잠시 후 다른 스레드가 태그를 생성한 것처럼 시뮬레이션
                                Thread {
                                    Thread.sleep(10)
                                    repository.save(duplicateTag)
                                }.start()

                                // 조금 기다린 후 block 실행
                                Thread.sleep(20)

                                // 이제 태그가 이미 존재하므로 block 실행 시 문제 없음
                                return block()
                            }
                            return block()
                        }

                        override fun tryLock(
                            key: String,
                            leaseTime: Duration,
                        ): Boolean = true

                        override fun unlock(key: String) = Unit
                    }

                val tagService =
                    createTagService(
                        repository = repository,
                        idGenerator = idGenerator,
                        clockTime = clockHolder.now(),
                        lockManager = customLockManager,
                    )

                // When: 태그 생성 시도
                val result = tagService.findOrCreateTags(listOf(TagName("duplicate")))

                // Then: 기존 태그가 반환됨
                result shouldHaveSize 1
                result[0].id shouldBe TagId(4000L)
                result[0].name shouldBe TagName("duplicate")
                repository.count() shouldBe 1
            }
        }

        describe("락 획득 패턴 검증") {

            it("각 태그에 대해 개별적으로 락을 획득한다") {
                // Given: 락 추적 환경
                val repository = FakeTagRepository()
                val lockManager = TrackingLockManager()
                val tagService =
                    createTagService(
                        repository = repository,
                        lockManager = lockManager,
                    )

                // When: 여러 태그 생성
                val tagNames =
                    listOf(
                        TagName("tag1"),
                        TagName("tag2"),
                        TagName("tag3"),
                    )
                tagService.findOrCreateTags(tagNames)

                // Then: 각 태그별로 개별 락 키 사용
                lockManager.getLockCount("tag:create:tag1") shouldBe 1
                lockManager.getLockCount("tag:create:tag2") shouldBe 1
                lockManager.getLockCount("tag:create:tag3") shouldBe 1
            }

            it("이미 존재하는 태그는 락을 획득하지 않는다") {
                // Given: 일부 태그가 이미 존재
                val repository = FakeTagRepository()
                val existingTag =
                    Tag.create(
                        id = TagId(1L),
                        name = TagName("existing"),
                        now = Instant.now(),
                    )
                repository.save(existingTag)

                val lockManager = TrackingLockManager()
                val tagService =
                    createTagService(
                        repository = repository,
                        lockManager = lockManager,
                    )

                // When: 기존 태그와 새 태그 혼합
                val tagNames =
                    listOf(
                        // 이미 존재
                        TagName("existing"),
                        // 새로 생성
                        TagName("new1"),
                        // 새로 생성
                        TagName("new2"),
                    )
                tagService.findOrCreateTags(tagNames)

                // Then: 새 태그에 대해서만 락 획득
                lockManager.getLockCount("tag:create:existing") shouldBe 0
                lockManager.getLockCount("tag:create:new1") shouldBe 1
                lockManager.getLockCount("tag:create:new2") shouldBe 1
            }
        }

        describe("생성 시간 처리") {

            it("모든 태그가 동일한 생성 시간을 갖는다") {
                // Given: 고정된 시간
                val fixedTime = Instant.parse("2025-01-25T15:30:00Z")
                val repository = FakeTagRepository()
                val tagService =
                    createTagService(
                        repository = repository,
                        clockTime = fixedTime,
                    )

                // When: 여러 태그 생성
                val tagNames =
                    listOf(
                        TagName("tag1"),
                        TagName("tag2"),
                        TagName("tag3"),
                    )
                val result = tagService.findOrCreateTags(tagNames)

                // Then: 모든 태그가 같은 시간에 생성됨
                result.forEach { tag ->
                    tag.createdAt shouldBe fixedTime
                    tag.updatedAt shouldBe fixedTime
                }
            }
        }

        describe("postCount 초기값") {

            it("새로 생성된 태그의 postCount는 0이다") {
                // Given: TagService
                val repository = FakeTagRepository()
                val tagService = createTagService(repository = repository)

                // When: 새 태그 생성
                val result = tagService.findOrCreateTags(listOf(TagName("newtag")))

                // Then: postCount가 0으로 초기화
                result[0].postCount shouldBe 0
            }
        }
    }
})
