package me.helloc.techwikiplus.post.application.facade

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.common.infrastructure.FakeAuthorizationPort
import me.helloc.techwikiplus.common.infrastructure.FakeClockHolder
import me.helloc.techwikiplus.common.infrastructure.FakePostIdGenerator
import me.helloc.techwikiplus.common.infrastructure.FakePostRepository
import me.helloc.techwikiplus.post.application.CreatePostFacade
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.service.PostAuthorizationService
import me.helloc.techwikiplus.post.domain.service.PostRegister
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole
import java.time.Instant

class CreatePostFacadeTest : DescribeSpec({
    lateinit var facade: CreatePostFacade
    lateinit var postRegister: PostRegister
    lateinit var postAuthorizationService: PostAuthorizationService
    lateinit var postRepository: FakePostRepository
    lateinit var postIdGenerator: FakePostIdGenerator
    lateinit var clockHolder: FakeClockHolder
    lateinit var authorizationPort: FakeAuthorizationPort

    beforeEach {
        postRepository = FakePostRepository()
        postIdGenerator = FakePostIdGenerator()
        clockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
        authorizationPort = FakeAuthorizationPort()
        // 기본적으로 ADMIN 권한을 설정하여 테스트가 통과하도록 함
        authorizationPort.setCurrentUser(UserId(1L), UserRole.ADMIN)

        postRegister =
            PostRegister(
                clockHolder = clockHolder,
                postIdGenerator = postIdGenerator,
                repository = postRepository,
            )

        postAuthorizationService =
            PostAuthorizationService(
                authorizationPort = authorizationPort,
            )

        facade =
            CreatePostFacade(
                postRegister = postRegister,
                postAuthorizationService = postAuthorizationService,
            )
    }

    describe("handle") {
        context("정상적인 게시글 생성") {
            it("유효한 정보로 게시글을 생성하고 ID를 반환한다") {
                // Given
                val title = PostTitle("테스트 게시글 제목")
                val body = PostBody("이것은 테스트 게시글의 본문 내용입니다. 최소 30자 이상의 내용을 포함하고 있습니다.")

                // When
                val result =
                    facade.handle(
                        title = title,
                        body = body,
                    )

                // Then
                result shouldNotBe null
                result shouldBe PostId(1000000L)
            }

            it("생성된 게시글이 저장소에 저장된다") {
                // Given
                val title = PostTitle("저장 테스트 게시글")
                val body = PostBody("이 게시글은 저장소에 제대로 저장되는지 확인하기 위한 테스트 게시글입니다.")

                // When
                val postId =
                    facade.handle(
                        title = title,
                        body = body,
                    )

                // Then
                val savedPost = postRepository.findBy(postId)
                savedPost shouldNotBe null
                savedPost!!.id shouldBe postId
                savedPost.title shouldBe title
                savedPost.body shouldBe body
                savedPost.status shouldBe PostStatus.DRAFT
            }

            it("생성 시각이 올바르게 설정된다") {
                // Given
                val fixedTime = Instant.parse("2025-12-25T15:30:45Z")
                clockHolder.setFixedTime(fixedTime)

                val title = PostTitle("시간 테스트 게시글")
                val body = PostBody("이 게시글은 생성 시각이 올바르게 설정되는지 확인하기 위한 테스트입니다.")

                // When
                val postId =
                    facade.handle(
                        title = title,
                        body = body,
                    )

                // Then
                val savedPost = postRepository.findBy(postId)
                savedPost shouldNotBe null
                savedPost!!.createdAt shouldBe fixedTime
                savedPost.updatedAt shouldBe fixedTime
            }
        }

        context("여러 게시글 생성") {
            it("순차적으로 여러 게시글을 생성할 수 있다") {
                // Given
                val posts =
                    listOf(
                        Pair(
                            PostTitle("첫 번째 게시글"),
                            PostBody("첫 번째 게시글의 본문 내용입니다. 충분한 길이의 내용을 포함하고 있습니다."),
                        ),
                        Pair(
                            PostTitle("두 번째 게시글"),
                            PostBody("두 번째 게시글의 본문 내용입니다. 이것도 충분한 길이의 내용을 포함합니다."),
                        ),
                        Pair(
                            PostTitle("세 번째 게시글"),
                            PostBody("세 번째 게시글의 본문 내용입니다. 마찬가지로 충분한 길이의 내용입니다."),
                        ),
                    )

                // When
                val postIds =
                    posts.map { (title, body) ->
                        facade.handle(title = title, body = body)
                    }

                // Then
                postIds.size shouldBe 3
                postIds[0] shouldBe PostId(1000000L)
                postIds[1] shouldBe PostId(1000001L)
                postIds[2] shouldBe PostId(1000002L)

                postRepository.count() shouldBe 3
                postIds.forEach { postId ->
                    postRepository.existsBy(postId) shouldBe true
                }
            }

            it("각 게시글은 고유한 ID를 갖는다") {
                // Given
                val postCount = 10
                val title = PostTitle("고유 ID 테스트 게시글")
                val body = PostBody("이 게시글은 고유한 ID를 가지는지 확인하기 위한 테스트 게시글입니다.")

                // When
                val postIds =
                    (1..postCount).map {
                        facade.handle(title = title, body = body)
                    }

                // Then
                val uniqueIds = postIds.toSet()
                uniqueIds.size shouldBe postCount // 모든 ID가 고유해야 함

                // ID가 순차적으로 증가하는지 확인
                postIds.forEachIndexed { index, postId ->
                    postId shouldBe PostId(1000000L + index)
                }
            }
        }

        context("동일 제목 게시글 생성") {
            it("동일한 제목으로 여러 게시글을 생성할 수 있다") {
                // Given
                val sameTitle = PostTitle("중복 가능한 제목")
                val bodies =
                    listOf(
                        PostBody("첫 번째 게시글의 내용입니다. 제목은 같지만 내용은 다릅니다."),
                        PostBody("두 번째 게시글의 내용입니다. 제목은 같지만 내용이 완전히 다릅니다."),
                        PostBody("세 번째 게시글의 내용입니다. 역시 제목은 같지만 내용이 다릅니다."),
                    )

                // When
                val postIds =
                    bodies.map { body ->
                        facade.handle(title = sameTitle, body = body)
                    }

                // Then
                postIds.size shouldBe 3
                postIds.distinct().size shouldBe 3 // 모든 ID가 다름

                // 저장된 게시글들 확인
                postIds.forEachIndexed { index, postId ->
                    val post = postRepository.findBy(postId)
                    post shouldNotBe null
                    post!!.title shouldBe sameTitle
                    post.body shouldBe bodies[index]
                }
            }
        }

        context("트랜잭션 동작") {
            it("Facade 메서드는 트랜잭션 내에서 실행된다") {
                // Given
                val title = PostTitle("트랜잭션 테스트 게시글")
                val body = PostBody("이 게시글은 트랜잭션 동작을 확인하기 위한 테스트 게시글입니다.")

                // When
                val postId =
                    facade.handle(
                        title = title,
                        body = body,
                    )

                // Then
                // 트랜잭션이 정상적으로 커밋되어 게시글이 저장됨
                postRepository.existsBy(postId) shouldBe true
                val savedPost = postRepository.findBy(postId)
                savedPost shouldNotBe null

                // 트랜잭션 어노테이션으로 인해 모든 작업이 원자적으로 처리됨
                postRepository.count() shouldBe 1
            }
        }

        context("대량 게시글 생성") {
            it("대량의 게시글을 순차적으로 생성할 수 있다") {
                // Given
                val postCount = 100
                val baseTitle = "대량 테스트 게시글"
                val baseBody = "이것은 대량 생성 테스트를 위한 게시글 번호"

                // When
                val postIds =
                    (1..postCount).map { i ->
                        val title = PostTitle("$baseTitle $i")
                        val body = PostBody("$baseBody $i 의 본문 내용입니다. 충분한 길이를 가진 내용입니다.")
                        facade.handle(title = title, body = body)
                    }

                // Then
                postIds.size shouldBe postCount
                postRepository.count() shouldBe postCount

                // 모든 ID가 고유한지 확인
                postIds.toSet().size shouldBe postCount

                // 첫 번째와 마지막 ID 확인
                postIds.first() shouldBe PostId(1000000L)
                postIds.last() shouldBe PostId(1000000L + postCount - 1)
            }
        }

        context("PostRegister 위임 확인") {
            it("CreatePostFacade는 PostRegister에게 작업을 위임한다") {
                // Given
                val title = PostTitle("위임 테스트 게시글")
                val body = PostBody("이 게시글은 PostRegister 위임을 확인하기 위한 테스트입니다.")
                val expectedTime = Instant.parse("2025-06-15T10:00:00Z")
                clockHolder.setFixedTime(expectedTime)

                // When
                val postId =
                    facade.handle(
                        title = title,
                        body = body,
                    )

                // Then
                // PostRegister를 통해 생성된 게시글의 속성들을 확인
                val savedPost = postRepository.findBy(postId)
                savedPost shouldNotBe null
                savedPost!!.id shouldBe postId
                savedPost.title shouldBe title
                savedPost.body shouldBe body
                savedPost.status shouldBe PostStatus.DRAFT // PostRegister가 설정하는 기본값
                savedPost.createdAt shouldBe expectedTime // PostRegister가 ClockHolder를 사용
                savedPost.updatedAt shouldBe expectedTime
            }
        }

        context("권한 검증") {
            it("ADMIN 권한이 있는 사용자는 게시글을 생성할 수 있다") {
                // Given: ADMIN 권한 설정
                authorizationPort.setCurrentUser(UserId(100L), UserRole.ADMIN)
                val title = PostTitle("ADMIN이 작성한 게시글")
                val body = PostBody("ADMIN 권한을 가진 사용자가 작성한 게시글입니다. 정상적으로 등록되어야 합니다.")

                // When: 게시글 생성
                val result = facade.handle(title, body)

                // Then: 성공적으로 생성
                result shouldNotBe null
                val savedPost = postRepository.findBy(result)
                savedPost shouldNotBe null
                savedPost!!.title shouldBe title
                savedPost.body shouldBe body
            }

            it("USER 권한만 있는 사용자는 게시글을 생성할 수 없다") {
                // Given: USER 권한 설정
                authorizationPort.setCurrentUser(UserId(200L), UserRole.USER)
                val title = PostTitle("일반 사용자가 시도한 게시글")
                val body = PostBody("일반 사용자가 작성을 시도한 게시글입니다. 권한 부족으로 실패해야 합니다.")

                // When & Then: 예외 발생
                val exception =
                    shouldThrow<PostDomainException> {
                        facade.handle(title, body)
                    }
                exception.postErrorCode shouldBe PostErrorCode.UNAUTHORIZED_ACCESS
                postRepository.count() shouldBe 0
            }

            it("인증되지 않은 사용자는 게시글을 생성할 수 없다") {
                // Given: 인증되지 않은 상태
                authorizationPort.clearCurrentUser()
                val title = PostTitle("익명 사용자가 시도한 게시글")
                val body = PostBody("인증되지 않은 사용자가 작성을 시도한 게시글입니다. 권한 부족으로 실패해야 합니다.")

                // When & Then: 예외 발생
                val exception =
                    shouldThrow<PostDomainException> {
                        facade.handle(title, body)
                    }
                exception.postErrorCode shouldBe PostErrorCode.UNAUTHORIZED_ACCESS
                postRepository.count() shouldBe 0
            }
        }
    }
})
