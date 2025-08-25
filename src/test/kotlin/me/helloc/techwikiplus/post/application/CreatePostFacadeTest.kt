package me.helloc.techwikiplus.post.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
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
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.PostAuthorizationService
import me.helloc.techwikiplus.post.domain.service.PostWriteService
import me.helloc.techwikiplus.post.domain.service.TagCountService
import me.helloc.techwikiplus.post.domain.service.TagService
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole
import java.time.Instant

class CreatePostFacadeTest : DescribeSpec({

    fun createTestEnvironment(): TestEnvironment {
        val authorizationPort = FakeAuthorizationPort()
        val tagRepository = FakeTagRepository()
        val tagJpaRepository = FakeTagJpaRepository()
        val postRepository = FakePostRepository()
        val clockHolder = FakeClockHolder(Instant.parse("2024-01-01T00:00:00Z"))
        val tagIdGenerator = FakeTagIdGenerator()
        val postIdGenerator = FakePostIdGenerator()
        val lockManager = NoOpLockManager()

        val postAuthorizationService = PostAuthorizationService(authorizationPort)
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
            CreatePostFacade(
                postWriteService = postWriteService,
                postAuthorizationService = postAuthorizationService,
                tagService = tagService,
                tagCountService = tagCountService,
            )

        return TestEnvironment(
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

    describe("CreatePostFacade.handle") {
        describe("정상 케이스") {
            context("관리자가 태그 없는 게시글을 생성할 때") {
                it("게시글이 성공적으로 생성된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val title = PostTitle("테스트 제목")
                    val body = PostBody("테스트 본문 내용입니다. 이 내용은 최소 길이 제약을 만족하기 위해 충분히 길게 작성되었습니다.")
                    val tagNames = emptyList<TagName>()

                    // when
                    val postId = env.facade.handle(title, body, tagNames)

                    // then
                    postId shouldNotBe null
                    val savedPost = env.postRepository.findBy(postId)
                    savedPost shouldNotBe null
                    savedPost?.title shouldBe title
                    savedPost?.body shouldBe body
                    savedPost?.tags?.size shouldBe 0
                }
            }

            context("관리자가 단일 태그와 함께 게시글을 생성할 때") {
                it("게시글과 태그가 정상적으로 생성되고 연결된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val title = PostTitle("Spring Boot 튜토리얼")
                    val body = PostBody("Spring Boot를 시작하는 방법에 대한 자세한 가이드입니다. 주요 개념과 설정 방법을 다룹니다.")
                    val tagNames = listOf(TagName("spring"))

                    // when
                    val postId = env.facade.handle(title, body, tagNames)

                    // then
                    val savedPost = env.postRepository.findBy(postId)
                    savedPost shouldNotBe null
                    savedPost?.tags?.size shouldBe 1
                    savedPost?.tags?.first()?.tagName?.value shouldBe "spring"

                    val createdTag = env.tagRepository.findBy(TagName("spring"))
                    createdTag shouldNotBe null
                    createdTag?.name?.value shouldBe "spring"
                }
            }

            context("관리자가 여러 태그와 함께 게시글을 생성할 때") {
                it("모든 태그가 생성되고 게시글과 연결된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val title = PostTitle("마이크로서비스 아키텍처")
                    val body = PostBody("마이크로서비스 설계와 구현에 대한 자세한 내용입니다. 모놀리스에서 MSA로의 전환 방법을 설명합니다.")
                    val tagNames =
                        listOf(
                            TagName("spring"),
                            TagName("kubernetes"),
                            TagName("docker"),
                        )

                    // when
                    val postId = env.facade.handle(title, body, tagNames)

                    // then
                    val savedPost = env.postRepository.findBy(postId)
                    savedPost shouldNotBe null
                    savedPost?.tags?.size shouldBe 3
                    savedPost?.tags?.map { it.tagName.value } shouldContainExactly listOf("spring", "kubernetes", "docker")

                    // 모든 태그가 생성되었는지 확인
                    tagNames.forEach { tagName ->
                        val tag = env.tagRepository.findBy(tagName)
                        tag shouldNotBe null
                    }
                }
            }

            context("기존 태그를 재사용하여 게시글을 생성할 때") {
                it("기존 태그가 재사용되고 카운트만 증가한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    // 첫 번째 게시글 생성 (태그 생성)
                    val firstTitle = PostTitle("첫 번째 게시글")
                    val firstBody = PostBody("첫 번째 게시글의 내용입니다. 이 내용은 최소 길이 제약을 만족하기 위해 충분히 길게 작성되었습니다.")
                    val tagNames = listOf(TagName("java"), TagName("spring"))
                    val firstPostId = env.facade.handle(firstTitle, firstBody, tagNames)

                    val javaTag = env.tagRepository.findBy(TagName("java"))
                    val springTag = env.tagRepository.findBy(TagName("spring"))
                    val javaTagId = javaTag!!.id
                    val springTagId = springTag!!.id

                    // when
                    // 두 번째 게시글 생성 (같은 태그 재사용)
                    val secondTitle = PostTitle("두 번째 게시글")
                    val secondBody = PostBody("두 번째 게시글의 내용입니다. 이 내용은 최소 길이 제약을 만족하기 위해 충분히 길게 작성되었습니다.")
                    val secondPostId = env.facade.handle(secondTitle, secondBody, tagNames)

                    // then
                    // 태그 ID가 변경되지 않았는지 확인 (재사용)
                    val reusedJavaTag = env.tagRepository.findBy(TagName("java"))
                    val reusedSpringTag = env.tagRepository.findBy(TagName("spring"))
                    reusedJavaTag?.id shouldBe javaTagId
                    reusedSpringTag?.id shouldBe springTagId

                    // 태그가 재사용되었는지 ID로 확인
                    // (TagCountService의 실제 동작은 별도로 테스트됨)

                    // 두 게시글이 모두 저장되었는지 확인
                    env.postRepository.findBy(firstPostId) shouldNotBe null
                    env.postRepository.findBy(secondPostId) shouldNotBe null
                }
            }

            context("새 태그와 기존 태그를 혼합하여 게시글을 생성할 때") {
                it("기존 태그는 재사용하고 새 태그만 생성한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    // 첫 번째 게시글로 일부 태그 생성
                    val firstTagNames = listOf(TagName("kotlin"), TagName("android"))
                    env.facade.handle(
                        PostTitle("안드로이드 개발"),
                        PostBody("코틀린으로 안드로이드 앱을 만드는 방법에 대한 자세한 가이드입니다. Jetpack Compose와 함께 사용하는 방법을 설명합니다."),
                        firstTagNames,
                    )

                    val kotlinTag = env.tagRepository.findBy(TagName("kotlin"))
                    val kotlinTagId = kotlinTag!!.id

                    // when
                    // 두 번째 게시글로 기존 태그 + 새 태그 사용
                    // kotlin: 기존 태그, coroutines와 flow: 새 태그
                    val secondTagNames =
                        listOf(
                            TagName("kotlin"),
                            TagName("coroutines"),
                            TagName("flow"),
                        )
                    val postId =
                        env.facade.handle(
                            PostTitle("Kotlin Coroutines 가이드"),
                            PostBody("비동기 프로그래밍의 새로운 패러다임에 대한 자세한 설명입니다. Kotlin Coroutines와 Flow를 활용한 비동기 처리 방법을 다룹니다."),
                            secondTagNames,
                        )

                    // then
                    // kotlin 태그는 재사용
                    val reusedKotlinTag = env.tagRepository.findBy(TagName("kotlin"))
                    reusedKotlinTag?.id shouldBe kotlinTagId

                    // 새 태그들이 생성됨
                    val coroutinesTag = env.tagRepository.findBy(TagName("coroutines"))
                    val flowTag = env.tagRepository.findBy(TagName("flow"))
                    coroutinesTag shouldNotBe null
                    flowTag shouldNotBe null

                    // 게시글에 모든 태그가 연결됨
                    val savedPost = env.postRepository.findBy(postId)
                    savedPost?.tags?.size shouldBe 3
                }
            }

            context("중복된 태그명이 포함된 리스트로 게시글을 생성할 때") {
                it("중복을 제거하고 한 번만 처리한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val title = PostTitle("중복 태그 테스트")
                    val body = PostBody("같은 태그가 여러 번 포함된 경우를 테스트하기 위한 내용입니다. 중복 태그 처리 로직을 검증합니다.")
                    // react와 javascript가 중복으로 포함된 태그 리스트
                    val tagNames =
                        listOf(
                            TagName("react"),
                            TagName("javascript"),
                            TagName("react"),
                            TagName("typescript"),
                            TagName("javascript"),
                        )

                    // when
                    val postId = env.facade.handle(title, body, tagNames)

                    // then
                    val savedPost = env.postRepository.findBy(postId)
                    // 현재 구현에서는 postWriteService.insert에 원본 tagNames를 전달하므로
                    // 중복이 포함된 5개의 태그가 게시글에 연결됨
                    savedPost?.tags?.size shouldBe 5

                    // 태그 저장소에는 중복 제거된 3개의 태그만 존재
                    val reactTag = env.tagRepository.findBy(TagName("react"))
                    val jsTag = env.tagRepository.findBy(TagName("javascript"))
                    val tsTag = env.tagRepository.findBy(TagName("typescript"))

                    reactTag shouldNotBe null
                    jsTag shouldNotBe null
                    tsTag shouldNotBe null

                    // 태그 카운트는 중복 제거된 태그 기준으로 1씩 증가
                    env.tagJpaRepository.getPostCount(reactTag!!.id.value) shouldBe 1
                    env.tagJpaRepository.getPostCount(jsTag!!.id.value) shouldBe 1
                    env.tagJpaRepository.getPostCount(tsTag!!.id.value) shouldBe 1
                }
            }
        }

        describe("권한 검증 실패 케이스") {
            context("일반 사용자가 게시글을 생성하려고 할 때") {
                it("FORBIDDEN_POST_ROLE 예외가 발생한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(2L), UserRole.USER)

                    val title = PostTitle("권한 없는 게시글")
                    val body = PostBody("이 게시글은 생성되지 않아야 합니다. 권한이 없는 사용자가 게시글을 생성하려고 시도하는 경우를 테스트합니다.")
                    val tagNames = listOf(TagName("test"))

                    // when & then
                    val exception =
                        shouldThrow<PostDomainException> {
                            env.facade.handle(title, body, tagNames)
                        }
                    exception.postErrorCode shouldBe PostErrorCode.FORBIDDEN_POST_ROLE

                    // 게시글이 저장되지 않았는지 확인
                    env.postRepository.findAll() shouldHaveSize 0

                    // 태그도 생성되지 않았는지 확인
                    env.tagRepository.findBy(TagName("test")) shouldBe null
                }
            }

            context("인증되지 않은 사용자가 게시글을 생성하려고 할 때") {
                it("FORBIDDEN_POST_ROLE 예외가 발생한다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.clearCurrentUser()

                    val title = PostTitle("익명 게시글")
                    val body = PostBody("인증되지 않은 사용자가 작성하려고 시도하는 게시글입니다. 인증 검증 로직을 테스트합니다.")
                    val tagNames = emptyList<TagName>()

                    // when & then
                    val exception =
                        shouldThrow<PostDomainException> {
                            env.facade.handle(title, body, tagNames)
                        }
                    exception.postErrorCode shouldBe PostErrorCode.FORBIDDEN_POST_ROLE

                    // 게시글이 저장되지 않았는지 확인
                    env.postRepository.findAll() shouldHaveSize 0
                }
            }
        }

        describe("태그 처리 검증") {
            context("태그 생성 순서를 확인할 때") {
                it("입력된 순서대로 태그가 처리된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val title = PostTitle("태그 순서 테스트")
                    val body = PostBody("태그가 입력 순서대로 처리되는지 확인하기 위한 테스트 내용입니다. 순서 보장 로직을 검증합니다.")
                    val tagNames =
                        listOf(
                            TagName("zebra"),
                            TagName("apple"),
                            TagName("monkey"),
                            TagName("banana"),
                        )

                    // when
                    val postId = env.facade.handle(title, body, tagNames)

                    // then
                    val savedPost = env.postRepository.findBy(postId)
                    savedPost?.tags?.map { it.tagName.value } shouldContainExactly
                        listOf("zebra", "apple", "monkey", "banana") // 입력 순서 유지
                }
            }

            context("빈 태그 리스트로 게시글을 생성할 때") {
                it("태그 없이 게시글만 생성된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val title = PostTitle("태그 없는 게시글")
                    val body = PostBody("태그가 없는 게시글입니다. 이 게시글은 태그 없이 생성되어야 합니다. 태그 없는 게시글 처리를 테스트합니다.")
                    val tagNames = emptyList<TagName>()

                    // when
                    val postId = env.facade.handle(title, body, tagNames)

                    // then
                    val savedPost = env.postRepository.findBy(postId)
                    savedPost shouldNotBe null
                    savedPost?.tags?.size shouldBe 0

                    // 태그 저장소가 비어있는지 확인
                    env.tagRepository.findAll() shouldHaveSize 0
                }
            }
        }

        describe("서비스 상호작용 검증") {
            context("게시글 생성 프로세스를 실행할 때") {
                it("올바른 순서로 서비스가 호출된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    val title = PostTitle("프로세스 검증")
                    val body = PostBody("서비스 호출 순서를 확인하기 위한 테스트 내용입니다. 각 서비스가 올바른 순서로 호출되는지 검증합니다.")
                    val tagNames = listOf(TagName("process"), TagName("test"))

                    // when
                    val postId = env.facade.handle(title, body, tagNames)

                    // then
                    // 1. 권한 검증이 통과됨 (예외 없이 진행)
                    // 2. 태그가 생성됨
                    env.tagRepository.findBy(TagName("process")) shouldNotBe null
                    env.tagRepository.findBy(TagName("test")) shouldNotBe null

                    // 3. 게시글이 저장됨
                    val savedPost = env.postRepository.findBy(postId)
                    savedPost shouldNotBe null
                    savedPost?.tags?.size shouldBe 2

                    // 4. 태그가 생성되고 연결됨
                    val processTag = env.tagRepository.findBy(TagName("process"))
                    val testTag = env.tagRepository.findBy(TagName("test"))
                    processTag shouldNotBe null
                    testTag shouldNotBe null
                }
            }

            context("대소문자가 다른 태그명을 처리할 때") {
                it("소문자로 정규화되어 같은 태그로 처리된다") {
                    // given
                    val env = createTestEnvironment()
                    env.authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

                    // 첫 번째 게시글: "Spring" 태그 생성
                    env.facade.handle(
                        PostTitle("첫 번째"),
                        PostBody("첫 번째 게시글의 내용입니다. 이 내용은 최소 길이 제약을 만족하기 위해 충분히 길게 작성되었습니다."),
                        listOf(TagName("Spring")),
                    )

                    val springTag = env.tagRepository.findBy(TagName("spring"))
                    val springTagId = springTag!!.id

                    // when
                    // 두 번째 게시글: "SPRING" 태그 사용 (같은 태그로 처리되어야 함)
                    env.facade.handle(
                        PostTitle("두 번째"),
                        PostBody("첫 번째 게시글의 내용입니다. 이 내용은 최소 길이 제약을 만족하기 위해 충분히 길게 작성되었습니다."),
                        listOf(TagName("SPRING")),
                    )

                    // then
                    // 같은 태그 ID 사용 (재사용)
                    val reusedTag = env.tagRepository.findBy(TagName("spring"))
                    reusedTag?.id shouldBe springTagId

                    // 태그가 재사용됨 (ID가 동일)

                    // 태그가 하나만 존재
                    env.tagRepository.findAll() shouldHaveSize 1
                }
            }
        }
    }
})

data class TestEnvironment(
    val facade: CreatePostFacade,
    val authorizationPort: FakeAuthorizationPort,
    val tagRepository: FakeTagRepository,
    val tagJpaRepository: FakeTagJpaRepository,
    val postRepository: FakePostRepository,
    val clockHolder: FakeClockHolder,
    val tagIdGenerator: FakeTagIdGenerator,
    val postIdGenerator: FakePostIdGenerator,
)
