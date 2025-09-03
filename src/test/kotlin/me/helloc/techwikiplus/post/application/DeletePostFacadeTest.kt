package me.helloc.techwikiplus.post.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.common.infrastructure.FakeAuthorizationPort
import me.helloc.techwikiplus.common.infrastructure.FakeClockHolder
import me.helloc.techwikiplus.common.infrastructure.FakePostIdGenerator
import me.helloc.techwikiplus.common.infrastructure.FakePostRepository
import me.helloc.techwikiplus.common.infrastructure.FakeTagIdGenerator
import me.helloc.techwikiplus.common.infrastructure.FakeTagJpaRepository
import me.helloc.techwikiplus.common.infrastructure.FakeTagRepository
import me.helloc.techwikiplus.common.infrastructure.NoOpLockManager
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostRevisionVersion
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.PostTag
import me.helloc.techwikiplus.post.domain.model.tag.Tag
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.PostAuthorizationService
import me.helloc.techwikiplus.post.domain.service.PostReadService
import me.helloc.techwikiplus.post.domain.service.PostWriteService
import me.helloc.techwikiplus.post.domain.service.TagCountService
import me.helloc.techwikiplus.post.domain.service.TagService
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole
import java.time.Instant

class DeletePostFacadeTest : DescribeSpec({

    fun createTestEnvironment(): DeletePostTestEnvironment {
        val authorizationPort = FakeAuthorizationPort()
        val tagRepository = FakeTagRepository()
        val tagJpaRepository = FakeTagJpaRepository()
        val postRepository = FakePostRepository()
        val clockHolder = FakeClockHolder(Instant.parse("2024-01-01T00:00:00Z"))
        val tagIdGenerator = FakeTagIdGenerator()
        val postIdGenerator = FakePostIdGenerator()
        val lockManager = NoOpLockManager()

        val postAuthorizationService = PostAuthorizationService(authorizationPort)
        val postReadService = PostReadService(postRepository)
        val tagService =
            TagService(
                tagRepository = tagRepository,
                tagIdGenerator = tagIdGenerator,
                clockHolder = clockHolder,
                lockManager = lockManager,
            )
        val postWriteService =
            PostWriteService(
                clockHolder = clockHolder,
                postIdGenerator = postIdGenerator,
                repository = postRepository,
            )
        val tagCountService = TagCountService(tagJpaRepository)

        val facade =
            DeletePostFacade(
                postReadService = postReadService,
                postWriteService = postWriteService,
                postAuthorizationService = postAuthorizationService,
            )

        return DeletePostTestEnvironment(
            facade = facade,
            authorizationPort = authorizationPort,
            tagRepository = tagRepository,
            tagJpaRepository = tagJpaRepository,
            postRepository = postRepository,
            clockHolder = clockHolder,
            tagIdGenerator = tagIdGenerator,
            postIdGenerator = postIdGenerator,
        )
    }

    fun createTestPost(
        env: DeletePostTestEnvironment,
        id: PostId = env.postIdGenerator.next(),
        title: String = "테스트 게시글",
        body: String = "테스트 게시글 본문입니다. 이 내용은 최소 길이 제약을 만족하기 위해 충분히 길게 작성되었습니다.",
        tags: Set<PostTag> = emptySet(),
        status: PostStatus = PostStatus.REVIEWED,
    ): Post {
        val post =
            Post.create(
                id = id,
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

    fun createTestTag(
        env: DeletePostTestEnvironment,
        name: String,
        postCount: Int = 0,
    ): Tag {
        val tag =
            Tag.create(
                id = env.tagIdGenerator.next(),
                name = TagName(name),
                now = env.clockHolder.now(),
            )
        env.tagRepository.save(tag)
        // FakeTagJpaRepository에 postCount 설정
        val tagWithCount = tag.copy(postCount = postCount)
        env.tagJpaRepository.addTagFromDomain(tagWithCount)
        return tagWithCount
    }

    describe("DeletePostFacade.handle") {
        describe("정상 케이스") {
            context("관리자가 태그 없는 게시글을 삭제할 때") {
                it("게시글이 DELETED 상태로 변경된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val post = createTestPost(env)

                    // when
                    env.facade.handle(post.id)

                    // then
                    val deletedPost = env.postRepository.findBy(post.id)
                    deletedPost shouldNotBe null
                    deletedPost?.status shouldBe PostStatus.DELETED
                    deletedPost?.title shouldBe post.title
                    deletedPost?.body shouldBe post.body
                }
            }

            context("관리자가 태그가 있는 게시글을 삭제할 때") {
                it("게시글이 DELETED 상태로 변경된다 (태그 카운트는 유지)") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val kotlinTag = createTestTag(env, "kotlin", 5)
                    val springTag = createTestTag(env, "spring", 3)
                    val tags =
                        setOf(
                            PostTag(kotlinTag.name, 1),
                            PostTag(springTag.name, 2),
                        )

                    val post = createTestPost(env, tags = tags)

                    // when
                    env.facade.handle(post.id)

                    // then
                    val deletedPost = env.postRepository.findBy(post.id)
                    deletedPost?.status shouldBe PostStatus.DELETED

                    // 태그 카운트 검증 - Soft Delete이므로 태그 카운트는 유지됨
                    env.tagJpaRepository.getPostCount(kotlinTag.id.value) shouldBe 5 // 변경 없음
                    env.tagJpaRepository.getPostCount(springTag.id.value) shouldBe 3 // 변경 없음
                }
            }

            context("여러 태그가 있는 게시글을 삭제할 때") {
                it("모든 태그의 카운트가 유지된다 (Soft Delete)") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val javaTag = createTestTag(env, "java", 10)
                    val springTag = createTestTag(env, "spring", 8)
                    val jpaTag = createTestTag(env, "jpa", 6)
                    val tags =
                        setOf(
                            PostTag(javaTag.name, 1),
                            PostTag(springTag.name, 2),
                            PostTag(jpaTag.name, 3),
                        )

                    val post = createTestPost(env, tags = tags)

                    // when
                    env.facade.handle(post.id)

                    // then
                    val deletedPost = env.postRepository.findBy(post.id)
                    deletedPost?.status shouldBe PostStatus.DELETED

                    // 모든 태그 카운트가 유지됨 (Soft Delete)
                    env.tagJpaRepository.getPostCount(javaTag.id.value) shouldBe 10 // 변경 없음
                    env.tagJpaRepository.getPostCount(springTag.id.value) shouldBe 8 // 변경 없음
                    env.tagJpaRepository.getPostCount(jpaTag.id.value) shouldBe 6 // 변경 없음
                }
            }

            context("이미 삭제된 게시글을 다시 삭제하려고 할 때") {
                it("POST_DELETED 예외가 발생한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val post = createTestPost(env, status = PostStatus.DELETED)

                    // when & then
                    val exception =
                        shouldThrow<PostDomainException> {
                            env.facade.handle(post.id)
                        }
                    exception.postErrorCode shouldBe PostErrorCode.POST_DELETED
                }
            }

            context("DRAFT 상태의 게시글을 삭제할 때") {
                it("정상적으로 DELETED 상태로 변경된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val post = createTestPost(env, status = PostStatus.DRAFT)

                    // when
                    env.facade.handle(post.id)

                    // then
                    val deletedPost = env.postRepository.findBy(post.id)
                    deletedPost?.status shouldBe PostStatus.DELETED
                }
            }

            context("IN_REVIEW 상태의 게시글을 삭제할 때") {
                it("정상적으로 DELETED 상태로 변경된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val post = createTestPost(env, status = PostStatus.IN_REVIEW)

                    // when
                    env.facade.handle(post.id)

                    // then
                    val deletedPost = env.postRepository.findBy(post.id)
                    deletedPost?.status shouldBe PostStatus.DELETED
                }
            }
        }

        describe("권한 검증 실패 케이스") {
            context("일반 사용자가 게시글을 삭제하려고 할 때") {
                it("FORBIDDEN_POST_ROLE 예외가 발생한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(2L), UserRole.USER)

                    val post = createTestPost(env)

                    // when & then
                    val exception =
                        shouldThrow<PostDomainException> {
                            env.facade.handle(post.id)
                        }
                    exception.postErrorCode shouldBe PostErrorCode.FORBIDDEN_POST_ROLE

                    // 게시글이 삭제되지 않았는지 확인
                    val unchangedPost = env.postRepository.findBy(post.id)
                    unchangedPost?.status shouldBe PostStatus.REVIEWED
                }
            }

            context("인증되지 않은 사용자가 게시글을 삭제하려고 할 때") {
                it("FORBIDDEN_POST_ROLE 예외가 발생한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.clearCurrentUser()

                    val post = createTestPost(env)

                    // when & then
                    val exception =
                        shouldThrow<PostDomainException> {
                            env.facade.handle(post.id)
                        }
                    exception.postErrorCode shouldBe PostErrorCode.FORBIDDEN_POST_ROLE
                }
            }
        }

        describe("게시글 조회 실패 케이스") {
            context("존재하지 않는 게시글을 삭제하려고 할 때") {
                it("POST_NOT_FOUND 예외가 발생한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

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

        describe("서비스 호출 순서 검증") {
            context("정상적인 삭제 프로세스를 실행할 때") {
                it("올바른 순서로 서비스가 호출된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val existingTag = createTestTag(env, "existing", 2)
                    val oldTags = setOf(PostTag(existingTag.name, 1))
                    val post = createTestPost(env, tags = oldTags)

                    // when
                    env.facade.handle(post.id)

                    // then
                    // 1. 권한 검증이 통과됨 (예외 없이 진행)
                    // 2. 게시글이 조회됨 (예외 없이 진행)
                    // 3. 게시글이 삭제됨 (DELETED 상태로 변경)
                    val deletedPost = env.postRepository.findBy(post.id)
                    deletedPost?.status shouldBe PostStatus.DELETED

                    // 4. 태그 카운트는 유지됨 (Soft Delete)
                    env.tagJpaRepository.getPostCount(existingTag.id.value) shouldBe 2 // 변경 없음
                }
            }
        }

        describe("Soft Delete 시 태그 카운트 유지") {
            context("태그 카운트가 1인 게시글을 삭제할 때") {
                it("태그 카운트가 유지된다 (Soft Delete)") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val rareTag = createTestTag(env, "rare", 1)
                    val tags = setOf(PostTag(rareTag.name, 1))
                    val post = createTestPost(env, tags = tags)

                    // when
                    env.facade.handle(post.id)

                    // then - Soft Delete이므로 태그 카운트 유지
                    env.tagJpaRepository.getPostCount(rareTag.id.value) shouldBe 1
                }
            }

            context("태그 카운트가 0인 태그를 가진 게시글을 삭제할 때") {
                it("태그 카운트가 그대로 0으로 유지된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val unusedTag = createTestTag(env, "unused", 0)
                    val tags = setOf(PostTag(unusedTag.name, 1))
                    val post = createTestPost(env, tags = tags)

                    // when
                    env.facade.handle(post.id)

                    // then - Soft Delete이므로 태그 카운트 유지
                    val count = env.tagJpaRepository.getPostCount(unusedTag.id.value)
                    count shouldBe 0 // 변경 없음
                }
            }
        }
    }
})

private data class DeletePostTestEnvironment(
    val facade: DeletePostFacade,
    val authorizationPort: FakeAuthorizationPort,
    val tagRepository: FakeTagRepository,
    val tagJpaRepository: FakeTagJpaRepository,
    val postRepository: FakePostRepository,
    val clockHolder: FakeClockHolder,
    val tagIdGenerator: FakeTagIdGenerator,
    val postIdGenerator: FakePostIdGenerator,
)
