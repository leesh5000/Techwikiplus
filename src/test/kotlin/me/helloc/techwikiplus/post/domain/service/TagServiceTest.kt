package me.helloc.techwikiplus.post.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.common.infrastructure.FakeTagRepository
import me.helloc.techwikiplus.common.infrastructure.NoOpLockManager
import me.helloc.techwikiplus.common.infrastructure.SlowLockManager
import me.helloc.techwikiplus.common.infrastructure.TrackingLockManager
import me.helloc.techwikiplus.post.domain.model.tag.Tag
import me.helloc.techwikiplus.post.domain.model.tag.TagId
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.user.domain.service.port.LockManagerException
import java.time.Instant

class TagServiceTest : FunSpec({

    lateinit var tagService: TagService
    lateinit var fakeTagRepository: FakeTagRepository
    lateinit var lockManager: NoOpLockManager

    beforeEach {
        fakeTagRepository = FakeTagRepository()
        lockManager = NoOpLockManager()

        tagService =
            TagService(
                tagRepository = fakeTagRepository,
                lockManager = lockManager,
            )
    }

    test("findOrCreateTags 호출 시 존재하지 않는 태그는 새로 생성되어야 한다") {
        // given
        val tagNames =
            listOf(
                TagName("spring"),
                TagName("kotlin"),
                TagName("jpa"),
            )

        // when
        val tags = tagService.findOrCreateTags(tagNames)

        // then
        tags shouldHaveSize 3
        tags[0].name.value shouldBe "spring"
        tags[1].name.value shouldBe "kotlin"
        tags[2].name.value shouldBe "jpa"
        tags.forEach { tag ->
            tag.postCount shouldBe 0
            tag.id shouldNotBe null
        }
    }

    test("findOrCreateTags 호출 시 이미 존재하는 태그는 조회되어야 한다") {
        // given
        val existingTag =
            Tag(
                id = TagId(1L),
                name = TagName("spring"),
                postCount = 5,
                createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2025-01-01T00:00:00Z"),
            )
        fakeTagRepository.save(existingTag)

        val tagNames =
            listOf(
                TagName("spring"),
                TagName("kotlin"),
            )

        // when
        val tags = tagService.findOrCreateTags(tagNames)

        // then
        tags shouldHaveSize 2
        tags[0].id shouldBe existingTag.id
        tags[0].postCount shouldBe 5
        tags[1].name.value shouldBe "kotlin"
        tags[1].postCount shouldBe 0
    }

    test("findOrCreateTags 호출 시 중복된 태그명은 하나만 처리되어야 한다") {
        // given
        val tagNames =
            listOf(
                TagName("spring"),
                TagName("kotlin"),
                TagName("spring"),
                TagName("jpa"),
                TagName("kotlin"),
            )

        // when
        val tags = tagService.findOrCreateTags(tagNames)

        // then
        tags shouldHaveSize 3
        val tagNameValues = tags.map { it.name.value }
        tagNameValues shouldContainExactly listOf("spring", "kotlin", "jpa")
    }

    test("findOrCreateTags 호출 시 빈 리스트가 들어오면 빈 리스트를 반환해야 한다") {
        // given
        val tagNames = emptyList<TagName>()

        // when
        val tags = tagService.findOrCreateTags(tagNames)

        // then
        tags shouldHaveSize 0
    }

    test("findOrCreateTags 호출 시 각 태그마다 락이 사용되어야 한다") {
        // given
        val trackingLockManager = TrackingLockManager()
        val tagServiceWithTracking =
            TagService(
                tagRepository = fakeTagRepository,
                lockManager = trackingLockManager,
            )

        val tagNames =
            listOf(
                TagName("spring"),
                TagName("kotlin"),
            )

        // when
        tagServiceWithTracking.findOrCreateTags(tagNames)

        // then
        trackingLockManager.getLockCount("tag:create:spring") shouldBe 1
        trackingLockManager.getLockCount("tag:create:kotlin") shouldBe 1
    }

    test("findOrCreateTags 호출 시 락 획득에 실패하면 예외가 발생해야 한다") {
        // given
        val slowLockManager = SlowLockManager()
        val tagServiceWithSlowLock =
            TagService(
                tagRepository = fakeTagRepository,
                lockManager = slowLockManager,
            )

        val tagNames = listOf(TagName("spring"))

        // when & then
        shouldThrow<LockManagerException> {
            tagServiceWithSlowLock.findOrCreateTags(tagNames)
        }
    }

    test("incrementPostCount 호출 시 태그의 게시글 수가 증가해야 한다") {
        // given
        val tag1 =
            Tag(
                id = TagId(1L),
                name = TagName("spring"),
                postCount = 0,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        val tag2 =
            Tag(
                id = TagId(2L),
                name = TagName("kotlin"),
                postCount = 3,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        fakeTagRepository.save(tag1)
        fakeTagRepository.save(tag2)

        // when
        tagService.incrementPostCount(listOf(tag1, tag2))

        // then
        val updatedTag1 = fakeTagRepository.findById(TagId(1L))
        val updatedTag2 = fakeTagRepository.findById(TagId(2L))
        updatedTag1?.postCount shouldBe 1
        updatedTag2?.postCount shouldBe 4
    }

    test("incrementPostCount 호출 시 빈 리스트가 들어오면 아무 작업도 하지 않아야 한다") {
        // given
        val tag =
            Tag(
                id = TagId(1L),
                name = TagName("spring"),
                postCount = 5,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        fakeTagRepository.save(tag)

        // when
        tagService.incrementPostCount(emptyList())

        // then
        val unchangedTag = fakeTagRepository.findById(TagId(1L))
        unchangedTag?.postCount shouldBe 5
    }

    test("decrementPostCount 호출 시 태그의 게시글 수가 감소해야 한다") {
        // given
        val tag1 =
            Tag(
                id = TagId(1L),
                name = TagName("spring"),
                postCount = 5,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        val tag2 =
            Tag(
                id = TagId(2L),
                name = TagName("kotlin"),
                postCount = 3,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        fakeTagRepository.save(tag1)
        fakeTagRepository.save(tag2)

        // when
        tagService.decrementPostCount(listOf(tag1, tag2))

        // then
        val updatedTag1 = fakeTagRepository.findById(TagId(1L))
        val updatedTag2 = fakeTagRepository.findById(TagId(2L))
        updatedTag1?.postCount shouldBe 4
        updatedTag2?.postCount shouldBe 2
    }

    test("decrementPostCount 호출 시 빈 리스트가 들어오면 아무 작업도 하지 않아야 한다") {
        // given
        val tag =
            Tag(
                id = TagId(1L),
                name = TagName("spring"),
                postCount = 5,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        fakeTagRepository.save(tag)

        // when
        tagService.decrementPostCount(emptyList())

        // then
        val unchangedTag = fakeTagRepository.findById(TagId(1L))
        unchangedTag?.postCount shouldBe 5
    }

    test("findByName 호출 시 태그명으로 태그를 조회할 수 있어야 한다") {
        // given
        val tag =
            Tag(
                id = TagId(1L),
                name = TagName("spring"),
                postCount = 10,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        fakeTagRepository.save(tag)

        // when
        val foundTag = tagService.findByName(TagName("spring"))

        // then
        foundTag shouldNotBe null
        foundTag?.id shouldBe TagId(1L)
        foundTag?.name?.value shouldBe "spring"
        foundTag?.postCount shouldBe 10
    }

    test("findByName 호출 시 존재하지 않는 태그명이면 null을 반환해야 한다") {
        // when
        val foundTag = tagService.findByName(TagName("nonexistent"))

        // then
        foundTag shouldBe null
    }

    test("findByNames 호출 시 여러 태그명으로 태그들을 조회할 수 있어야 한다") {
        // given
        val tag1 =
            Tag(
                id = TagId(1L),
                name = TagName("spring"),
                postCount = 10,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        val tag2 =
            Tag(
                id = TagId(2L),
                name = TagName("kotlin"),
                postCount = 5,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        val tag3 =
            Tag(
                id = TagId(3L),
                name = TagName("jpa"),
                postCount = 3,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        fakeTagRepository.save(tag1)
        fakeTagRepository.save(tag2)
        fakeTagRepository.save(tag3)

        val tagNames =
            listOf(
                TagName("spring"),
                TagName("jpa"),
                TagName("nonexistent"),
            )

        // when
        val foundTags = tagService.findByNames(tagNames)

        // then
        foundTags shouldHaveSize 2
        foundTags[0].name.value shouldBe "spring"
        foundTags[1].name.value shouldBe "jpa"
    }

    test("findByNames 호출 시 빈 리스트가 들어오면 빈 리스트를 반환해야 한다") {
        // given
        val tag =
            Tag(
                id = TagId(1L),
                name = TagName("spring"),
                postCount = 10,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        fakeTagRepository.save(tag)

        // when
        val foundTags = tagService.findByNames(emptyList())

        // then
        foundTags shouldHaveSize 0
    }
})
