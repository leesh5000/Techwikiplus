package me.helloc.techwikiplus.post.domain.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.common.infrastructure.FakeClockHolder
import me.helloc.techwikiplus.common.infrastructure.FakePostIdGenerator
import me.helloc.techwikiplus.common.infrastructure.FakePostRepository
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import java.time.Instant

class PostWriteServiceTest : DescribeSpec({

    /**
     * 테스트용 PostRegister 생성 헬퍼 메서드
     * FIRST 원칙의 Independent를 위해 매번 새로운 인스턴스 생성
     */
    fun createPostWriteService(
        repository: FakePostRepository = FakePostRepository(),
        clockTime: Instant = Instant.parse("2025-01-07T10:00:00Z"),
        postIdGenerator: FakePostIdGenerator = FakePostIdGenerator(),
    ): PostWriteService {
        return PostWriteService(
            clockHolder = FakeClockHolder(clockTime),
            postIdGenerator = postIdGenerator,
            repository = repository,
        )
    }

    describe("PostRegister") {

        context("정상적인 게시글 등록") {
            it("유효한 정보로 게시글을 성공적으로 등록한다") {
                // Given: 유효한 게시글 정보와 PostRegister 준비
                val repository = FakePostRepository()
                val postRegister = createPostWriteService(repository = repository)
                val title = PostTitle("테스트 게시글 제목")
                val body = PostBody("이것은 테스트 게시글의 본문 내용입니다. 최소 30자 이상이어야 합니다.")

                // When: 게시글 등록 실행
                val result = postRegister.insert(title, body)

                // Then: 게시글이 성공적으로 등록되었는지 검증
                result shouldNotBe null
                result.id shouldBe PostId(1000000L)
                result.title shouldBe title
                result.body shouldBe body
                result.status shouldBe PostStatus.DRAFT
                result.createdAt shouldBe Instant.parse("2025-01-07T10:00:00Z")
                result.updatedAt shouldBe Instant.parse("2025-01-07T10:00:00Z")

                val savedPost = repository.findBy(PostId(1000000L))
                savedPost shouldNotBe null
                savedPost?.id shouldBe result.id
            }

            it("생성 시각이 올바르게 설정된다") {
                // Given: 특정 시간으로 설정된 ClockHolder와 PostRegister
                val specificTime = Instant.parse("2025-12-25T15:30:45Z")
                val repository = FakePostRepository()
                val postRegister =
                    createPostWriteService(
                        repository = repository,
                        clockTime = specificTime,
                    )
                val title = PostTitle("크리스마스 게시글")
                val body = PostBody("크리스마스에 작성하는 특별한 게시글입니다. 시간이 정확히 기록되어야 합니다.")

                // When: 게시글 등록 실행
                val result = postRegister.insert(title, body)

                // Then: 생성 시각이 올바르게 설정되었는지 검증
                result.createdAt shouldBe specificTime
                result.updatedAt shouldBe specificTime
            }

            it("생성된 게시글 상태가 DRAFT로 설정된다") {
                // Given: PostRegister와 게시글 정보 준비
                val repository = FakePostRepository()
                val postRegister = createPostWriteService(repository = repository)
                val title = PostTitle("초안 상태 테스트")
                val body = PostBody("새로 생성되는 게시글은 항상 DRAFT 상태로 시작해야 합니다.")

                // When: 게시글 등록 실행
                val result = postRegister.insert(title, body)

                // Then: 상태가 DRAFT로 설정되었는지 검증
                result.status shouldBe PostStatus.DRAFT
                result.isDraft() shouldBe true
                result.isInReview() shouldBe false
                result.isReviewed() shouldBe false
            }
        }

        context("순차 다중 게시글 등록") {
            it("여러 게시글을 순차적으로 등록할 수 있다") {
                // Given: 두 개의 서로 다른 게시글 정보 준비
                val repository = FakePostRepository()
                val clockHolder = FakeClockHolder(Instant.parse("2025-01-07T10:00:00Z"))
                val postIdGenerator = FakePostIdGenerator()
                val postRegister =
                    createPostWriteService(
                        repository = repository,
                        clockTime = clockHolder.now(),
                        postIdGenerator = postIdGenerator,
                    )

                val title1 = PostTitle("첫 번째 게시글")
                val body1 = PostBody("첫 번째 게시글의 내용입니다. 30자 이상의 충분한 내용을 포함합니다.")

                val title2 = PostTitle("두 번째 게시글")
                val body2 = PostBody("두 번째 게시글의 내용입니다. 이것도 30자 이상의 충분한 내용을 포함합니다.")

                // When: 두 게시글을 순차적으로 등록
                val post1 = postRegister.insert(title1, body1)

                // 시간 경과 시뮬레이션
                clockHolder.advanceTimeBySeconds(60)
                val postRegister2 =
                    createPostWriteService(
                        repository = repository,
                        clockTime = clockHolder.now(),
                        postIdGenerator = postIdGenerator,
                    )
                val post2 = postRegister2.insert(title2, body2)

                // Then: 두 게시글 모두 성공적으로 등록됨
                post1.id shouldBe PostId(1000000L)
                post2.id shouldBe PostId(1000001L)

                repository.count() shouldBe 2
                repository.findBy(PostId(1000000L)) shouldBe post1
                repository.findBy(PostId(1000001L)) shouldBe post2
            }

            it("각 게시글은 고유한 ID를 갖는다") {
                // Given: PostRegister와 여러 게시글 정보 준비
                val repository = FakePostRepository()
                val postIdGenerator = FakePostIdGenerator(startId = 5000000L)
                val postRegister =
                    createPostWriteService(
                        repository = repository,
                        postIdGenerator = postIdGenerator,
                    )

                // When: 3개의 게시글을 연속으로 등록
                val posts =
                    (1..3).map { i ->
                        val title = PostTitle("게시글 번호 $i")
                        val body = PostBody("게시글 번호 $i 의 본문 내용입니다. 각 게시글은 고유한 ID를 가져야 합니다.")
                        postRegister.insert(title, body)
                    }

                // Then: 각 게시글이 고유한 ID를 가지는지 검증
                posts[0].id shouldBe PostId(5000000L)
                posts[1].id shouldBe PostId(5000001L)
                posts[2].id shouldBe PostId(5000002L)

                // ID 중복이 없는지 확인
                val uniqueIds = posts.map { it.id }.toSet()
                uniqueIds.size shouldBe 3
            }
        }

        context("동일 제목 게시글 등록") {
            it("동일한 제목으로 여러 게시글을 생성할 수 있다") {
                // Given: 동일한 제목을 가진 여러 게시글 정보 준비
                val repository = FakePostRepository()
                val postRegister = createPostWriteService(repository = repository)
                val sameTitle = PostTitle("중복 가능한 제목")
                val body1 = PostBody("첫 번째 게시글의 내용입니다. 제목은 같지만 내용은 다릅니다.")
                val body2 = PostBody("두 번째 게시글의 내용입니다. 제목은 같지만 내용이 완전히 다릅니다.")

                // When: 동일한 제목으로 두 게시글 등록
                val post1 = postRegister.insert(sameTitle, body1)
                val post2 = postRegister.insert(sameTitle, body2)

                // Then: 두 게시글 모두 성공적으로 등록되고 서로 다른 ID를 가짐
                post1.title shouldBe sameTitle
                post2.title shouldBe sameTitle
                post1.id shouldNotBe post2.id
                post1.body shouldNotBe post2.body

                repository.count() shouldBe 2
                repository.findBy(post1.id) shouldBe post1
                repository.findBy(post2.id) shouldBe post2
            }
        }

        context("저장소 상호작용") {
            it("게시글이 저장소에 올바르게 저장된다") {
                // Given: PostRegister와 게시글 정보 준비
                val repository = FakePostRepository()
                val postRegister = createPostWriteService(repository = repository)
                val title = PostTitle("저장소 테스트 게시글")
                val body = PostBody("이 게시글은 저장소에 제대로 저장되는지 확인하기 위한 테스트입니다.")

                // When: 게시글 등록 실행
                val result = postRegister.insert(title, body)

                // Then: 저장소에서 게시글을 조회할 수 있는지 검증
                repository.existsBy(result.id) shouldBe true
                repository.findBy(result.id) shouldBe result
                repository.count() shouldBe 1
                repository.getAll().first() shouldBe result
            }

            it("여러 게시글이 저장소에 누적된다") {
                // Given: PostRegister와 여러 게시글 정보 준비
                val repository = FakePostRepository()
                val postRegister = createPostWriteService(repository = repository)

                // When: 5개의 게시글을 등록
                val posts =
                    (1..5).map { i ->
                        val title = PostTitle("누적 테스트 게시글 $i")
                        val body = PostBody("이것은 누적 테스트를 위한 게시글 번호 $i 의 본문 내용입니다.")
                        postRegister.insert(title, body)
                    }

                // Then: 모든 게시글이 저장소에 누적되었는지 검증
                repository.count() shouldBe 5
                repository.getAll().size shouldBe 5
                posts.forEach { post ->
                    repository.existsBy(post.id) shouldBe true
                    repository.findBy(post.id) shouldBe post
                }
            }
        }
    }
})
