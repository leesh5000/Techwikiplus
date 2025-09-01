package me.helloc.techwikiplus.post.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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

class UpdatePostFacadeTest : DescribeSpec({

    fun createTestEnvironment(): UpdatePostTestEnvironment {
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
            UpdatePostFacade(
                postReadService = postReadService,
                postWriteService = postWriteService,
                postAuthorizationService = postAuthorizationService,
                tagService = tagService,
                tagCountService = tagCountService,
            )

        return UpdatePostTestEnvironment(
            facade = facade,
            authorizationPort = authorizationPort,
            tagRepository = tagRepository,
            tagJpaRepository = tagJpaRepository,
            postRepository = postRepository,
            clockHolder = clockHolder,
            tagIdGenerator = tagIdGenerator,
            postIdGenerator = postIdGenerator,
            postReadService = postReadService,
            postWriteService = postWriteService,
            tagService = tagService,
            tagCountService = tagCountService,
        )
    }

    fun createTestPost(
        env: UpdatePostTestEnvironment,
        id: PostId = env.postIdGenerator.next(),
        title: String = "기존 제목",
        body: String = "기존 본문 내용입니다. 이 내용은 최소 길이 제약을 만족하기 위해 충분히 길게 작성되었습니다.",
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
        env: UpdatePostTestEnvironment,
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

    describe("UpdatePostFacade.handle") {
        describe("정상 케이스") {
            context("관리자가 태그 없는 게시글을 수정할 때") {
                it("게시글 제목과 본문이 성공적으로 수정된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val post = createTestPost(env)
                    val newTitle = PostTitle("수정된 제목")
                    val newBody = PostBody("수정된 본문 내용입니다. 이 내용은 최소 길이 제약을 만족하기 위해 충분히 길게 작성되었습니다.")
                    val newTags = emptyList<TagName>()

                    // when
                    env.facade.handle(post.id, newTitle, newBody, newTags)

                    // then
                    val updatedPost = env.postRepository.findBy(post.id)
                    updatedPost shouldNotBe null
                    updatedPost?.title shouldBe newTitle
                    updatedPost?.body shouldBe newBody
                    updatedPost?.tags?.size shouldBe 0
                }
            }

            context("관리자가 태그가 있는 게시글에서 태그를 모두 제거할 때") {
                it("게시글이 수정되고 기존 태그의 카운트가 감소한다") {
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
                    val newTitle = PostTitle("태그 제거된 게시글")
                    val newBody = PostBody("태그가 모두 제거된 게시글입니다. 이제 태그가 없습니다. 충분한 길이의 본문을 작성합니다.")
                    val newTags = emptyList<TagName>()

                    // when
                    env.facade.handle(post.id, newTitle, newBody, newTags)

                    // then
                    val updatedPost = env.postRepository.findBy(post.id)
                    updatedPost?.tags?.size shouldBe 0

                    // 태그 카운트 검증 - 제거된 태그의 카운트가 감소
                    env.tagJpaRepository.getPostCount(kotlinTag.id.value) shouldBe 4 // 5 - 1
                    env.tagJpaRepository.getPostCount(springTag.id.value) shouldBe 2 // 3 - 1
                }
            }

            context("관리자가 태그 없는 게시글에 새 태그를 추가할 때") {
                it("게시글이 수정되고 새 태그가 생성되며 카운트가 증가한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val post = createTestPost(env)
                    val newTitle = PostTitle("태그 추가된 게시글")
                    val newBody = PostBody("새로운 태그가 추가된 게시글입니다. React와 TypeScript 태그를 추가합니다.")
                    val newTags = listOf(TagName("react"), TagName("typescript"))

                    // when
                    env.facade.handle(post.id, newTitle, newBody, newTags)

                    // then
                    val updatedPost = env.postRepository.findBy(post.id)
                    updatedPost?.tags?.size shouldBe 2
                    updatedPost?.tags?.map { it.tagName.value } shouldContainExactlyInAnyOrder listOf("react", "typescript")

                    // 새 태그가 생성되었는지 확인
                    val reactTag = env.tagRepository.findBy(TagName("react"))
                    val tsTag = env.tagRepository.findBy(TagName("typescript"))
                    reactTag shouldNotBe null
                    tsTag shouldNotBe null

                    // 태그 카운트 검증 - 추가된 태그의 카운트가 증가
                    env.tagJpaRepository.getPostCount(reactTag!!.id.value) shouldBe 1 // 0 + 1
                    env.tagJpaRepository.getPostCount(tsTag!!.id.value) shouldBe 1 // 0 + 1
                }
            }

            context("관리자가 게시글의 태그를 완전히 교체할 때") {
                it("기존 태그는 제거되고 새 태그가 추가되며 카운트가 적절히 조정된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val javaTag = createTestTag(env, "java", 10)
                    val springTag = createTestTag(env, "spring", 5)
                    val oldTags =
                        setOf(
                            PostTag(javaTag.name, 1),
                            PostTag(springTag.name, 2),
                        )

                    val post = createTestPost(env, tags = oldTags)
                    val newTitle = PostTitle("태그 교체된 게시글")
                    val newBody = PostBody("Java/Spring에서 Kotlin/Ktor로 마이그레이션한 게시글입니다. 기술 스택이 완전히 변경되었습니다.")
                    val newTags = listOf(TagName("kotlin"), TagName("ktor"))

                    // when
                    env.facade.handle(post.id, newTitle, newBody, newTags)

                    // then
                    val updatedPost = env.postRepository.findBy(post.id)
                    updatedPost?.tags?.size shouldBe 2
                    updatedPost?.tags?.map { it.tagName.value } shouldContainExactlyInAnyOrder listOf("kotlin", "ktor")

                    // 새 태그가 생성되었는지 확인
                    val kotlinTag = env.tagRepository.findBy(TagName("kotlin"))
                    val ktorTag = env.tagRepository.findBy(TagName("ktor"))
                    kotlinTag shouldNotBe null
                    ktorTag shouldNotBe null

                    // 태그 카운트 검증
                    // 기존 태그 (java, spring): 제거되므로 감소
                    env.tagJpaRepository.getPostCount(javaTag.id.value) shouldBe 9 // 10 - 1
                    env.tagJpaRepository.getPostCount(springTag.id.value) shouldBe 4 // 5 - 1

                    // 새 태그 (kotlin, ktor): 추가되므로 증가
                    env.tagJpaRepository.getPostCount(kotlinTag!!.id.value) shouldBe 1 // 0 + 1
                    env.tagJpaRepository.getPostCount(ktorTag!!.id.value) shouldBe 1 // 0 + 1
                }
            }

            context("관리자가 일부 태그만 변경할 때") {
                it("공통 태그는 유지되고 추가/제거된 태그의 카운트만 조정된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val kotlinTag = createTestTag(env, "kotlin", 7)
                    val springTag = createTestTag(env, "spring", 4)
                    val oldTags =
                        setOf(
                            PostTag(kotlinTag.name, 1),
                            PostTag(springTag.name, 2),
                        )

                    val post = createTestPost(env, tags = oldTags)
                    val newTitle = PostTitle("일부 태그 변경")
                    val newBody = PostBody("Kotlin은 유지하고 Spring을 제거하며 Coroutines를 추가한 게시글입니다. 부분적인 태그 변경을 테스트합니다.")
                    // kotlin 유지, spring 제거, coroutines 추가
                    val newTags = listOf(TagName("kotlin"), TagName("coroutines"))

                    // when
                    env.facade.handle(post.id, newTitle, newBody, newTags)

                    // then
                    val updatedPost = env.postRepository.findBy(post.id)
                    updatedPost?.tags?.size shouldBe 2
                    updatedPost?.tags?.map { it.tagName.value } shouldContainExactlyInAnyOrder listOf("kotlin", "coroutines")

                    val coroutinesTag = env.tagRepository.findBy(TagName("coroutines"))
                    coroutinesTag shouldNotBe null

                    // 태그 카운트 검증
                    // kotlin: 유지되므로 변경 없음
                    env.tagJpaRepository.getPostCount(kotlinTag.id.value) shouldBe 7 // 변경 없음

                    // spring: 제거되므로 감소
                    env.tagJpaRepository.getPostCount(springTag.id.value) shouldBe 3 // 4 - 1

                    // coroutines: 추가되므로 증가
                    env.tagJpaRepository.getPostCount(coroutinesTag!!.id.value) shouldBe 1 // 0 + 1
                }
            }

            context("대소문자가 다른 태그명으로 수정할 때") {
                it("태그명이 소문자로 정규화되어 처리된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val reactTag = createTestTag(env, "react", 3)
                    val oldTags = setOf(PostTag(reactTag.name, 1))

                    val post = createTestPost(env, tags = oldTags)
                    val newTitle = PostTitle("대소문자 테스트")
                    val newBody = PostBody("React, REACT, ReAcT 모두 같은 태그로 처리되어야 합니다. 대소문자 정규화를 테스트합니다.")
                    // 대소문자 섞인 태그명 제공
                    val newTags = listOf(TagName("REACT"), TagName("TypeScript"))

                    // when
                    env.facade.handle(post.id, newTitle, newBody, newTags)

                    // then
                    val updatedPost = env.postRepository.findBy(post.id)
                    updatedPost?.tags?.size shouldBe 2
                    // 소문자로 정규화되어 저장됨
                    updatedPost?.tags?.map { it.tagName.value } shouldContainExactlyInAnyOrder listOf("react", "typescript")

                    // react 태그는 재사용됨
                    val reusedReactTag = env.tagRepository.findBy(TagName("react"))
                    reusedReactTag?.id shouldBe reactTag.id

                    // typescript 태그는 새로 생성됨 (소문자로)
                    val tsTag = env.tagRepository.findBy(TagName("typescript"))
                    tsTag shouldNotBe null
                }
            }

            context("중복된 태그명이 포함된 리스트로 수정할 때") {
                it("중복이 제거되고 유니크한 태그만 처리된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val post = createTestPost(env)
                    val newTitle = PostTitle("중복 태그 테스트")
                    val newBody = PostBody("같은 태그가 여러 번 포함된 경우를 테스트합니다. 중복은 제거되어야 합니다.")
                    // 중복된 태그명 포함
                    val newTags =
                        listOf(
                            TagName("javascript"),
                            TagName("react"),
                            // 중복
                            TagName("javascript"),
                            // 중복
                            TagName("react"),
                            TagName("nodejs"),
                        )

                    // when
                    env.facade.handle(post.id, newTitle, newBody, newTags)

                    // then
                    val updatedPost = env.postRepository.findBy(post.id)
                    // 중복이 제거되어 3개만 저장됨
                    updatedPost?.tags?.size shouldBe 3

                    // 태그 저장소에는 중복 제거된 3개만 존재
                    env.tagRepository.findBy(TagName("javascript")) shouldNotBe null
                    env.tagRepository.findBy(TagName("react")) shouldNotBe null
                    env.tagRepository.findBy(TagName("nodejs")) shouldNotBe null
                }
            }
        }

        describe("권한 검증 실패 케이스") {
            context("일반 사용자가 게시글을 수정하려고 할 때") {
                it("FORBIDDEN_POST_ROLE 예외가 발생한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(2L), UserRole.USER)

                    val post = createTestPost(env)
                    val newTitle = PostTitle("수정 시도")
                    val newBody = PostBody("일반 사용자는 게시글을 수정할 수 없습니다. 권한 검증 테스트를 위한 본문입니다.")
                    val newTags = emptyList<TagName>()

                    // when & then
                    val exception =
                        shouldThrow<PostDomainException> {
                            env.facade.handle(post.id, newTitle, newBody, newTags)
                        }
                    exception.postErrorCode shouldBe PostErrorCode.FORBIDDEN_POST_ROLE

                    // 게시글이 수정되지 않았는지 확인
                    val unchangedPost = env.postRepository.findBy(post.id)
                    unchangedPost?.title?.value shouldBe "기존 제목"
                    unchangedPost?.body?.value shouldBe "기존 본문 내용입니다. 이 내용은 최소 길이 제약을 만족하기 위해 충분히 길게 작성되었습니다."
                }
            }

            context("인증되지 않은 사용자가 게시글을 수정하려고 할 때") {
                it("FORBIDDEN_POST_ROLE 예외가 발생한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.clearCurrentUser()

                    val post = createTestPost(env)
                    val newTitle = PostTitle("익명 수정")
                    val newBody = PostBody("인증되지 않은 사용자의 수정 시도입니다. 이 수정은 실패해야 합니다.")
                    val newTags = emptyList<TagName>()

                    // when & then
                    val exception =
                        shouldThrow<PostDomainException> {
                            env.facade.handle(post.id, newTitle, newBody, newTags)
                        }
                    exception.postErrorCode shouldBe PostErrorCode.FORBIDDEN_POST_ROLE
                }
            }
        }

        describe("게시글 조회 실패 케이스") {
            context("존재하지 않는 게시글을 수정하려고 할 때") {
                it("POST_NOT_FOUND 예외가 발생한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val nonExistentId = PostId(999L)
                    val newTitle = PostTitle("존재하지 않는 게시글")
                    val newBody = PostBody("이 게시글은 존재하지 않으므로 수정할 수 없습니다. 예외 처리를 테스트합니다.")
                    val newTags = emptyList<TagName>()

                    // when & then
                    val exception =
                        shouldThrow<PostDomainException> {
                            env.facade.handle(nonExistentId, newTitle, newBody, newTags)
                        }
                    exception.postErrorCode shouldBe PostErrorCode.POST_NOT_FOUND
                    exception.params[0] shouldBe nonExistentId
                }
            }
        }

        describe("서비스 호출 순서 검증") {
            context("정상적인 수정 프로세스를 실행할 때") {
                it("올바른 순서로 서비스가 호출된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val existingTag = createTestTag(env, "existing", 2)
                    val oldTags = setOf(PostTag(existingTag.name, 1))
                    val post = createTestPost(env, tags = oldTags)

                    val newTitle = PostTitle("프로세스 검증")
                    val newBody = PostBody("서비스 호출 순서를 확인하기 위한 테스트입니다. 각 단계가 올바른 순서로 실행되는지 검증합니다.")
                    val newTags = listOf(TagName("process"), TagName("test"))

                    // when
                    env.facade.handle(post.id, newTitle, newBody, newTags)

                    // then
                    // 1. 권한 검증이 통과됨 (예외 없이 진행)
                    // 2. 게시글이 조회됨 (예외 없이 진행)
                    // 3. 태그가 생성/조회됨
                    env.tagRepository.findBy(TagName("process")) shouldNotBe null
                    env.tagRepository.findBy(TagName("test")) shouldNotBe null

                    // 4. 게시글이 수정됨
                    val updatedPost = env.postRepository.findBy(post.id)
                    updatedPost?.title shouldBe newTitle
                    updatedPost?.body shouldBe newBody
                    updatedPost?.tags?.size shouldBe 2

                    // 5. 태그 카운트가 조정됨
                    // existing 태그는 제거되므로 감소
                    env.tagJpaRepository.getPostCount(existingTag.id.value) shouldBe 1 // 2 - 1
                }
            }
        }

        describe("빈 태그 리스트 처리") {
            context("기존 태그가 있는 게시글을 빈 태그 리스트로 수정할 때") {
                it("모든 태그가 제거된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val tag1 = createTestTag(env, "tag1", 1)
                    val tag2 = createTestTag(env, "tag2", 1)
                    val oldTags =
                        setOf(
                            PostTag(tag1.name, 1),
                            PostTag(tag2.name, 2),
                        )

                    val post = createTestPost(env, tags = oldTags)
                    val newTitle = PostTitle("태그 제거")
                    val newBody = PostBody("모든 태그를 제거한 게시글입니다. 태그 없이 게시글만 존재합니다.")
                    val newTags = emptyList<TagName>()

                    // when
                    env.facade.handle(post.id, newTitle, newBody, newTags)

                    // then
                    val updatedPost = env.postRepository.findBy(post.id)
                    updatedPost?.tags?.size shouldBe 0

                    // 태그 자체는 남아있지만 카운트가 감소
                    env.tagJpaRepository.getPostCount(tag1.id.value) shouldBe 0 // 1 - 1
                    env.tagJpaRepository.getPostCount(tag2.id.value) shouldBe 0 // 1 - 1
                }
            }
        }
    }
})

private data class UpdatePostTestEnvironment(
    val facade: UpdatePostFacade,
    val authorizationPort: FakeAuthorizationPort,
    val tagRepository: FakeTagRepository,
    val tagJpaRepository: FakeTagJpaRepository,
    val postRepository: FakePostRepository,
    val clockHolder: FakeClockHolder,
    val tagIdGenerator: FakeTagIdGenerator,
    val postIdGenerator: FakePostIdGenerator,
    val postReadService: PostReadService,
    val postWriteService: PostWriteService,
    val tagService: TagService,
    val tagCountService: TagCountService,
)
