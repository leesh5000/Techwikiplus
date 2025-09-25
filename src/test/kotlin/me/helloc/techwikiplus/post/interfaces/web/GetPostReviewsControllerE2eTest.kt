package me.helloc.techwikiplus.post.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters.Companion.builder
import com.epages.restdocs.apispec.Schema.Companion.schema
import me.helloc.techwikiplus.common.config.BaseE2eTest
import me.helloc.techwikiplus.common.config.annotations.E2eTest
import me.helloc.techwikiplus.common.infrastructure.security.jwt.JwtTokenManager
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostRevisionVersion
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.review.PostReview
import me.helloc.techwikiplus.post.domain.model.review.PostReviewStatus
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.service.port.PostIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import me.helloc.techwikiplus.post.domain.service.port.PostReviewIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostReviewRepository
import me.helloc.techwikiplus.post.dto.response.PostReviewResponse
import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.user.domain.model.Nickname
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole
import me.helloc.techwikiplus.user.domain.model.UserStatus
import me.helloc.techwikiplus.user.domain.service.port.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Instant

/**
 * GetPostReviewsController E2E 테스트
 *
 * - 특정 게시글의 리뷰 목록 조회 API 테스트
 * - TestContainers를 통한 실제 DB 연동
 * - API 문서 자동 생성
 */
@E2eTest(generateDocs = true)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-post",
        "spring.application.version=1.0.0-INTEGRATION",
        "api.documentation.enabled=true",
    ],
)
class GetPostReviewsControllerE2eTest : BaseE2eTest() {
    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var postReviewRepository: PostReviewRepository

    @Autowired
    private lateinit var postIdGenerator: PostIdGenerator

    @Autowired
    private lateinit var postReviewIdGenerator: PostReviewIdGenerator

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenManager: JwtTokenManager

    @Test
    fun `GET posts-postId-reviews - 게시글의 리뷰 목록을 조회할 수 있다`() {
        // Given - 게시글 생성
        val post = createTestPost()

        // 여러 리뷰 생성 (현재 구조상 하나만 활성화 가능하지만, 테스트를 위해 상태 변경)
        val review1 = createTestReview(post.id, PostReviewStatus.COMPLETED)
        val review2 = createTestReview(post.id, PostReviewStatus.CANCELLED)
        val review3 = createTestReview(post.id, PostReviewStatus.IN_REVIEW)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/reviews", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].postId").value(post.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].startedAt").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].deadline").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").exists())
            .andDo(
                documentWithResource(
                    "게시글 리뷰 목록 조회",
                    builder()
                        .tag("Post Review")
                        .summary("게시글 리뷰 목록 조회")
                        .description(
                            """
                            특정 게시글에 대한 모든 리뷰 목록을 조회합니다.
                            
                            리뷰 목록은 시작 시간 기준 내림차순으로 정렬되어 반환됩니다.
                            - 최신 리뷰가 먼저 표시됩니다
                            - IN_REVIEW, COMPLETED, CANCELLED 등 모든 상태의 리뷰가 포함됩니다
                            - 삭제된 게시글의 리뷰도 조회 가능합니다
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("리뷰 목록을 조회할 게시글 ID"),
                        )
                        .responseFields(
                            fieldWithPath("[]")
                                .type(JsonFieldType.ARRAY)
                                .description("리뷰 목록"),
                            fieldWithPath("[].id")
                                .type(JsonFieldType.NUMBER)
                                .description("리뷰 ID"),
                            fieldWithPath("[].postId")
                                .type(JsonFieldType.NUMBER)
                                .description("게시글 ID"),
                            fieldWithPath("[].startedAt")
                                .type(JsonFieldType.STRING)
                                .description("검수 시작 시간 (ISO-8601)"),
                            fieldWithPath("[].deadline")
                                .type(JsonFieldType.STRING)
                                .description("검수 마감 시간 (ISO-8601)"),
                            fieldWithPath("[].status")
                                .type(JsonFieldType.STRING)
                                .description("검수 상태 (IN_REVIEW, COMPLETED, CANCELLED)"),
                            fieldWithPath("[].winningRevisionId")
                                .type(JsonFieldType.NUMBER)
                                .description("선정된 수정본 ID (완료된 경우)").optional(),
                        )
                        .responseSchema(
                            schema(
                                "List<${PostReviewResponse::class.simpleName}>",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts-postId-reviews - 리뷰가 없는 게시글은 빈 배열을 반환한다`() {
        // Given - 리뷰가 없는 게시글
        val post = createTestPost()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/reviews", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isEmpty)
            .andDo(
                documentWithResource(
                    "게시글 리뷰 목록 조회 - 빈 목록",
                    builder()
                        .tag("Post Review")
                        .summary("게시글 리뷰 목록 조회 - 리뷰 없음")
                        .description(
                            """
                            리뷰가 없는 게시글을 조회하면 빈 배열이 반환됩니다.
                            
                            404 에러가 아닌 200 OK와 함께 빈 배열 []이 반환됩니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("리뷰 목록을 조회할 게시글 ID"),
                        )
                        .responseFields(
                            fieldWithPath("[]")
                                .type(JsonFieldType.ARRAY)
                                .description("빈 리뷰 목록"),
                        )
                        .responseSchema(
                            schema(
                                "EmptyList",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts-postId-reviews - 존재하지 않는 게시글도 빈 배열을 반환한다`() {
        // Given - 존재하지 않는 게시글 ID
        val nonExistentPostId = 999999L

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/reviews", nonExistentPostId)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isEmpty)
            .andDo(
                documentWithResource(
                    "게시글 리뷰 목록 조회 - 존재하지 않는 게시글",
                    builder()
                        .tag("Post Review")
                        .summary("게시글 리뷰 목록 조회 - 게시글 없음")
                        .description(
                            """
                            존재하지 않는 게시글 ID로 조회해도 빈 배열이 반환됩니다.
                            
                            이는 리뷰가 없는 경우와 동일한 응답입니다.
                            게시글 존재 여부와 관계없이 해당 게시글의 리뷰 목록만 조회합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("리뷰 목록을 조회할 게시글 ID"),
                        )
                        .responseFields(
                            fieldWithPath("[]")
                                .type(JsonFieldType.ARRAY)
                                .description("빈 리뷰 목록"),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts-postId-reviews - 삭제된 게시글의 리뷰도 조회할 수 있다`() {
        // Given - 삭제된 게시글과 리뷰
        val deletedPost = createTestPost(status = PostStatus.DELETED)
        val review = createTestReview(deletedPost.id, PostReviewStatus.COMPLETED)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/reviews", deletedPost.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(review.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("COMPLETED"))
            .andDo(
                documentWithResource(
                    "게시글 리뷰 목록 조회 - 삭제된 게시글",
                    builder()
                        .tag("Post Review")
                        .summary("삭제된 게시글의 리뷰 목록 조회")
                        .description(
                            """
                            삭제된 게시글(DELETED 상태)의 리뷰도 정상적으로 조회됩니다.
                            
                            게시글이 삭제되어도 리뷰 이력은 보존되며,
                            관리자나 감사 목적으로 조회할 수 있습니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("리뷰 목록을 조회할 게시글 ID"),
                        )
                        .responseFields(
                            fieldWithPath("[]")
                                .type(JsonFieldType.ARRAY)
                                .description("리뷰 목록"),
                            fieldWithPath("[].id")
                                .type(JsonFieldType.NUMBER)
                                .description("리뷰 ID"),
                            fieldWithPath("[].postId")
                                .type(JsonFieldType.NUMBER)
                                .description("게시글 ID"),
                            fieldWithPath("[].startedAt")
                                .type(JsonFieldType.STRING)
                                .description("검수 시작 시간"),
                            fieldWithPath("[].deadline")
                                .type(JsonFieldType.STRING)
                                .description("검수 마감 시간"),
                            fieldWithPath("[].status")
                                .type(JsonFieldType.STRING)
                                .description("검수 상태"),
                            fieldWithPath("[].winningRevisionId")
                                .type(JsonFieldType.NUMBER)
                                .description("선정된 수정본 ID").optional(),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts-postId-reviews - 여러 상태의 리뷰가 시간순으로 정렬되어 반환된다`() {
        // Given - 여러 상태의 리뷰들을 다른 시간에 생성
        val post = createTestPost()

        val now = Instant.now()
        // 2시간 전
        val review1 = createTestReviewWithTime(post.id, PostReviewStatus.COMPLETED, now.minusSeconds(7200))
        // 1시간 전
        val review2 = createTestReviewWithTime(post.id, PostReviewStatus.CANCELLED, now.minusSeconds(3600))
        // 현재
        val review3 = createTestReviewWithTime(post.id, PostReviewStatus.IN_REVIEW, now)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/reviews", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
            // 최신순 정렬 확인 (review3 -> review2 -> review1)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(review3.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("IN_REVIEW"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(review2.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].status").value("CANCELLED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].id").value(review1.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].status").value("COMPLETED"))
            .andDo(
                documentWithResource(
                    "게시글 리뷰 목록 조회 - 정렬 확인",
                    builder()
                        .tag("Post Review")
                        .summary("게시글 리뷰 목록 시간순 정렬")
                        .description(
                            """
                            게시글의 모든 리뷰가 시작 시간 기준 내림차순으로 정렬되어 반환됩니다.
                            
                            - 가장 최근에 시작된 리뷰가 첫 번째로 표시됩니다
                            - IN_REVIEW, COMPLETED, CANCELLED 등 모든 상태가 포함됩니다
                            - 리뷰 상태와 관계없이 시작 시간만을 기준으로 정렬됩니다
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("리뷰 목록을 조회할 게시글 ID"),
                        )
                        .responseFields(
                            fieldWithPath("[]")
                                .type(JsonFieldType.ARRAY)
                                .description("시간순으로 정렬된 리뷰 목록"),
                            fieldWithPath("[].id")
                                .type(JsonFieldType.NUMBER)
                                .description("리뷰 ID"),
                            fieldWithPath("[].postId")
                                .type(JsonFieldType.NUMBER)
                                .description("게시글 ID"),
                            fieldWithPath("[].startedAt")
                                .type(JsonFieldType.STRING)
                                .description("검수 시작 시간 (정렬 기준)"),
                            fieldWithPath("[].deadline")
                                .type(JsonFieldType.STRING)
                                .description("검수 마감 시간"),
                            fieldWithPath("[].status")
                                .type(JsonFieldType.STRING)
                                .description("검수 상태"),
                            fieldWithPath("[].winningRevisionId")
                                .type(JsonFieldType.NUMBER)
                                .description("선정된 수정본 ID").optional(),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts-postId-reviews - 인증 없이도 리뷰 목록을 조회할 수 있다`() {
        // Given - 게시글과 리뷰
        val post = createTestPost()
        val review = createTestReview(post.id, PostReviewStatus.IN_REVIEW)

        // When & Then - 인증 헤더 없이 요청
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/reviews", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(review.id.value.toString()))
            .andDo(
                documentWithResource(
                    "게시글 리뷰 목록 조회 - 인증 불필요",
                    builder()
                        .tag("Post Review")
                        .summary("게시글 리뷰 목록 조회 (공개 API)")
                        .description(
                            """
                            리뷰 목록 조회는 공개 API로, 인증 없이 누구나 조회할 수 있습니다.
                            
                            게시글 리뷰는 공개 정보이므로 로그인하지 않은 사용자도
                            리뷰 현황을 확인할 수 있습니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("리뷰 목록을 조회할 게시글 ID"),
                        )
                        .responseFields(
                            fieldWithPath("[]")
                                .type(JsonFieldType.ARRAY)
                                .description("리뷰 목록"),
                            fieldWithPath("[].id")
                                .type(JsonFieldType.NUMBER)
                                .description("리뷰 ID"),
                            fieldWithPath("[].postId")
                                .type(JsonFieldType.NUMBER)
                                .description("게시글 ID"),
                            fieldWithPath("[].startedAt")
                                .type(JsonFieldType.STRING)
                                .description("검수 시작 시간"),
                            fieldWithPath("[].deadline")
                                .type(JsonFieldType.STRING)
                                .description("검수 마감 시간"),
                            fieldWithPath("[].status")
                                .type(JsonFieldType.STRING)
                                .description("검수 상태"),
                            fieldWithPath("[].winningRevisionId")
                                .type(JsonFieldType.NUMBER)
                                .description("선정된 수정본 ID").optional(),
                        )
                        .build(),
                ),
            )
    }

    /**
     * 테스트용 게시글 생성 헬퍼 메서드
     */
    private fun createTestPost(
        title: String = "테스트 게시글 ${System.nanoTime()}",
        status: PostStatus = PostStatus.DRAFT,
    ): Post {
        val now = Instant.now()
        val postId = postIdGenerator.next()
        val post =
            Post(
                id = postId,
                title = PostTitle(title),
                body = PostBody("이것은 테스트 게시글의 본문 내용입니다. 최소 30자 이상의 내용을 포함하고 있습니다."),
                status = status,
                version = PostRevisionVersion(1),
                createdAt = now,
                updatedAt = now,
                tags = emptySet(),
            )

        return postRepository.save(post)
    }

    /**
     * 테스트용 리뷰 생성 헬퍼 메서드
     */
    private fun createTestReview(
        postId: PostId,
        status: PostReviewStatus,
        startedBy: Long? = null,
    ): PostReview {
        val now = Instant.now()
        val review =
            PostReview(
                id = postReviewIdGenerator.generate(),
                postId = postId,
                startedAt = now,
                // 72시간
                deadline = now.plusSeconds(259200),
                status = status,
                winningRevisionId = if (status == PostReviewStatus.COMPLETED) PostRevisionId(System.nanoTime()) else null,
                startedBy = startedBy,
            )

        return postReviewRepository.save(review)
    }

    /**
     * 특정 시간에 생성된 리뷰 생성 헬퍼 메서드
     */
    private fun createTestReviewWithTime(
        postId: PostId,
        status: PostReviewStatus,
        startedAt: Instant,
    ): PostReview {
        val review =
            PostReview(
                id = postReviewIdGenerator.generate(),
                postId = postId,
                startedAt = startedAt,
                // 72시간
                deadline = startedAt.plusSeconds(259200),
                status = status,
                winningRevisionId = if (status == PostReviewStatus.COMPLETED) PostRevisionId(System.nanoTime()) else null,
                startedBy = null,
            )

        return postReviewRepository.save(review)
    }

    /**
     * 테스트용 사용자 생성 헬퍼 메서드
     */
    private fun createTestUser(
        email: String = "test${System.nanoTime()}@test.com",
        nickname: String = "User${System.nanoTime() % 100000}",
        role: UserRole = UserRole.USER,
    ): User {
        val now = Instant.now()
        val userId = UserId(System.nanoTime())
        val user =
            User(
                id = userId,
                email = Email(email),
                nickname = Nickname(nickname),
                encodedPassword = EncodedPassword("{noop}password123!"),
                status = UserStatus.ACTIVE,
                role = role,
                createdAt = now,
                updatedAt = now,
            )

        return userRepository.save(user)
    }

    // Kotlin DSL for assertions
    private infix fun Any?.shouldBe(expected: Any?) {
        if (this != expected) {
            throw AssertionError("Expected: $expected, but was: $this")
        }
    }

    private infix fun Any?.shouldNotBe(expected: Any?) {
        if (this == expected) {
            throw AssertionError("Expected not to be: $expected")
        }
    }

    private infix fun String.shouldContain(substring: String) {
        if (!this.contains(substring)) {
            throw AssertionError("Expected string to contain: $substring, but was: $this")
        }
    }
}
