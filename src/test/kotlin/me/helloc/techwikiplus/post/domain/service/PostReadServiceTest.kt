package me.helloc.techwikiplus.post.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.common.infrastructure.FakeClockHolder
import me.helloc.techwikiplus.common.infrastructure.FakePostIdGenerator
import me.helloc.techwikiplus.common.infrastructure.FakePostRepository
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.PostTag
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import java.time.Instant

class PostReadServiceTest : DescribeSpec({

    fun createTestEnvironment(): TestEnvironment {
        val postRepository = FakePostRepository()
        val postReadService = PostReadService(postRepository)
        val clockHolder = FakeClockHolder(Instant.parse("2024-01-01T00:00:00Z"))
        val postIdGenerator = FakePostIdGenerator()

        return TestEnvironment(
            postReadService = postReadService,
            postRepository = postRepository,
            clockHolder = clockHolder,
            postIdGenerator = postIdGenerator,
        )
    }

    fun createTestPost(
        env: TestEnvironment,
        title: String = "테스트 게시글",
        body: String = "테스트 게시글 본문입니다. 충분한 길이의 컨텐츠를 포함하고 있습니다.",
        status: PostStatus = PostStatus.REVIEWED,
        tags: List<PostTag> = emptyList(),
    ): Post {
        val post =
            Post(
                id = env.postIdGenerator.next(),
                title = PostTitle(title),
                body = PostBody(body),
                status = status,
                tags = tags,
                createdAt = env.clockHolder.now(),
                updatedAt = env.clockHolder.now(),
            )
        env.postRepository.save(post)
        return post
    }

    describe("PostReadService.getBy") {
        describe("정상 케이스") {
            context("존재하는 게시글을 조회할 때") {
                it("게시글을 정상적으로 반환한다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env)

                    // when
                    val result = env.postReadService.getBy(post.id)

                    // then
                    result shouldNotBe null
                    result.id shouldBe post.id
                    result.title shouldBe post.title
                    result.body shouldBe post.body
                    result.status shouldBe post.status
                }
            }

            context("태그가 포함된 게시글을 조회할 때") {
                it("태그 정보와 함께 게시글을 반환한다") {
                    // given
                    val env = createTestEnvironment()
                    val tags =
                        listOf(
                            PostTag(TagName("kotlin"), 1),
                            PostTag(TagName("spring"), 2),
                        )
                    val post =
                        createTestPost(
                            env,
                            title = "Kotlin Spring 가이드",
                            body = "Kotlin과 Spring을 함께 사용하는 방법에 대한 상세한 가이드입니다.",
                            tags = tags,
                        )

                    // when
                    val result = env.postReadService.getBy(post.id)

                    // then
                    result shouldNotBe null
                    result.tags.size shouldBe 2
                    result.tags[0].tagName.value shouldBe "kotlin"
                    result.tags[1].tagName.value shouldBe "spring"
                }
            }

            context("다양한 상태의 게시글이 존재할 때") {
                it("REVIEWED 상태의 게시글을 조회할 수 있다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env, status = PostStatus.REVIEWED)

                    // when
                    val result = env.postReadService.getBy(post.id)

                    // then
                    result shouldNotBe null
                    result.status shouldBe PostStatus.REVIEWED
                }

                it("DRAFT 상태의 게시글을 조회할 수 있다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env, status = PostStatus.DRAFT)

                    // when
                    val result = env.postReadService.getBy(post.id)

                    // then
                    result shouldNotBe null
                    result.status shouldBe PostStatus.DRAFT
                }

                it("IN_REVIEW 상태의 게시글을 조회할 수 있다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env, status = PostStatus.IN_REVIEW)

                    // when
                    val result = env.postReadService.getBy(post.id)

                    // then
                    result shouldNotBe null
                    result.status shouldBe PostStatus.IN_REVIEW
                }

                it("DELETED 상태의 게시글을 조회할 수 있다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env, status = PostStatus.DELETED)

                    // when
                    val result = env.postReadService.getBy(post.id)

                    // then
                    result shouldNotBe null
                    result.status shouldBe PostStatus.DELETED
                }
            }
        }

        describe("예외 케이스") {
            context("존재하지 않는 게시글을 조회할 때") {
                it("POST_NOT_FOUND 예외가 발생한다") {
                    // given
                    val env = createTestEnvironment()
                    val nonExistentId = PostId(999L)

                    // when & then
                    val exception =
                        shouldThrow<PostDomainException> {
                            env.postReadService.getBy(nonExistentId)
                        }
                    exception.postErrorCode shouldBe PostErrorCode.POST_NOT_FOUND
                    exception.params[0] shouldBe nonExistentId
                }
            }
        }

        describe("데이터 일관성 검증") {
            context("동일한 게시글을 여러 번 조회할 때") {
                it("항상 동일한 결과를 반환한다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env)

                    // when
                    val result1 = env.postReadService.getBy(post.id)
                    val result2 = env.postReadService.getBy(post.id)
                    val result3 = env.postReadService.getBy(post.id)

                    // then
                    result1.id shouldBe result2.id
                    result2.id shouldBe result3.id
                    result1.title shouldBe result2.title
                    result2.title shouldBe result3.title
                }
            }

            context("빈 태그 리스트를 가진 게시글을 조회할 때") {
                it("빈 태그 리스트와 함께 게시글을 반환한다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env, tags = emptyList())

                    // when
                    val result = env.postReadService.getBy(post.id)

                    // then
                    result shouldNotBe null
                    result.tags.size shouldBe 0
                }
            }
        }
    }
})

private data class TestEnvironment(
    val postReadService: PostReadService,
    val postRepository: FakePostRepository,
    val clockHolder: FakeClockHolder,
    val postIdGenerator: FakePostIdGenerator,
)
