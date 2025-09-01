package me.helloc.techwikiplus.post.application

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
import me.helloc.techwikiplus.post.domain.model.post.PostRevisionVersion
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.PostTag
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.PostReadService
import java.time.Instant

class ReadPostFacadeTest : DescribeSpec({

    fun createTestEnvironment(): ReadPostTestEnvironment {
        val postRepository = FakePostRepository()
        val postReadService = PostReadService(postRepository)
        val facade = ReadPostFacade(postReadService)
        val clockHolder = FakeClockHolder(Instant.parse("2024-01-01T00:00:00Z"))
        val postIdGenerator = FakePostIdGenerator()

        return ReadPostTestEnvironment(
            facade = facade,
            postRepository = postRepository,
            postReadService = postReadService,
            clockHolder = clockHolder,
            postIdGenerator = postIdGenerator,
        )
    }

    fun createTestPost(
        env: ReadPostTestEnvironment,
        title: String = "테스트 게시글",
        body: String = "테스트 게시글 본문입니다. 충분한 길이의 컨텐츠를 포함하고 있습니다.",
        status: PostStatus = PostStatus.REVIEWED,
        tags: Set<PostTag> = emptySet(),
    ): Post {
        val post =
            Post.create(
                id = env.postIdGenerator.next(),
                title = PostTitle(title),
                body = PostBody(body),
                status = status,
                version = PostRevisionVersion(),
                postTags = tags,
                createdAt = env.clockHolder.now(),
                updatedAt = env.clockHolder.now(),
            )
        env.postRepository.save(post)
        return post
    }

    describe("ReadPostFacade.handle") {
        describe("정상 케이스") {
            context("존재하는 게시글을 조회할 때") {
                it("게시글을 정상적으로 반환한다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env)

                    // when
                    val result = env.facade.handle(post.id)

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
                        setOf(
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
                    val result = env.facade.handle(post.id)

                    // then
                    result shouldNotBe null
                    result.tags.size shouldBe 2
                    val tagNames = result.tags.map { it.tagName.value }.toSet()
                    tagNames shouldBe setOf("kotlin", "spring")
                }
            }

            context("여러 상태의 게시글이 존재할 때") {
                it("REVIEWED 상태의 게시글은 조회 가능하다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env, status = PostStatus.REVIEWED)

                    // when
                    val result = env.facade.handle(post.id)

                    // then
                    result shouldNotBe null
                    result.status shouldBe PostStatus.REVIEWED
                }

                it("DRAFT 상태의 게시글도 조회 가능하다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env, status = PostStatus.DRAFT)

                    // when
                    val result = env.facade.handle(post.id)

                    // then
                    result shouldNotBe null
                    result.status shouldBe PostStatus.DRAFT
                }

                it("IN_REVIEW 상태의 게시글도 조회 가능하다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env, status = PostStatus.IN_REVIEW)

                    // when
                    val result = env.facade.handle(post.id)

                    // then
                    result shouldNotBe null
                    result.status shouldBe PostStatus.IN_REVIEW
                }

                it("DELETED 상태의 게시글도 조회 가능하다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env, status = PostStatus.DELETED)

                    // when
                    val result = env.facade.handle(post.id)

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
                            env.facade.handle(nonExistentId)
                        }
                    exception.postErrorCode shouldBe PostErrorCode.POST_NOT_FOUND
                    exception.params[0] shouldBe nonExistentId
                }
            }
        }

        describe("Facade와 Service 통합 검증") {
            context("Facade가 도메인 서비스를 올바르게 호출할 때") {
                it("도메인 서비스의 결과를 그대로 반환한다") {
                    // given
                    val env = createTestEnvironment()
                    val post =
                        createTestPost(
                            env,
                            title = "통합 테스트 게시글",
                            body = "Facade와 Service의 통합을 검증하는 테스트입니다.",
                        )

                    // when
                    val facadeResult = env.facade.handle(post.id)
                    val serviceResult = env.postReadService.getBy(post.id)

                    // then
                    facadeResult.id shouldBe serviceResult.id
                    facadeResult.title shouldBe serviceResult.title
                    facadeResult.body shouldBe serviceResult.body
                    facadeResult.status shouldBe serviceResult.status
                }
            }

            context("동일한 게시글을 여러 번 조회할 때") {
                it("항상 동일한 결과를 반환한다") {
                    // given
                    val env = createTestEnvironment()
                    val post = createTestPost(env)

                    // when
                    val result1 = env.facade.handle(post.id)
                    val result2 = env.facade.handle(post.id)
                    val result3 = env.facade.handle(post.id)

                    // then
                    result1.id shouldBe result2.id
                    result2.id shouldBe result3.id
                    result1.title shouldBe result2.title
                    result2.title shouldBe result3.title
                }
            }
        }
    }
})

private data class ReadPostTestEnvironment(
    val facade: ReadPostFacade,
    val postRepository: FakePostRepository,
    val postReadService: PostReadService,
    val clockHolder: FakeClockHolder,
    val postIdGenerator: FakePostIdGenerator,
)
