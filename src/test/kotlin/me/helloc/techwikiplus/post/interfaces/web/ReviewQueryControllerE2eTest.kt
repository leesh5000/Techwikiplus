package me.helloc.techwikiplus.post.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters.Companion.builder
import com.epages.restdocs.apispec.Schema.Companion.schema
import me.helloc.techwikiplus.common.config.BaseE2eTest
import me.helloc.techwikiplus.common.config.annotations.E2eTest
import me.helloc.techwikiplus.common.config.documentation.withStandardErrorResponse
import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.common.infrastructure.security.jwt.JwtTokenManager
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostRevisionVersion
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.review.PostReview
import me.helloc.techwikiplus.post.domain.model.review.PostReviewStatus
import me.helloc.techwikiplus.post.domain.model.review.PostRevision
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.service.port.PostIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import me.helloc.techwikiplus.post.domain.service.port.PostReviewIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostReviewRepository
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionRepository
import me.helloc.techwikiplus.post.interfaces.web.dto.PostReviewResponse
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
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Instant

/**
 * ReviewQueryController E2E 테스트
 *
 * - 전체 애플리케이션 컨텍스트 로드
 * - TestContainers를 통한 실제 DB 연동
 * - 운영 환경과 동일한 설정
 * - End-to-End 검증
 * - API 문서 자동 생성 (generateDocs = true)
 * - 리뷰 조회 API 테스트
 */
@E2eTest(generateDocs = true)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-review",
        "spring.application.version=1.0.0-INTEGRATION",
        "api.documentation.enabled=true",
    ],
)
class ReviewQueryControllerE2eTest : BaseE2eTest() {
    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var postReviewRepository: PostReviewRepository

    @Autowired
    private lateinit var postRevisionRepository: PostRevisionRepository

    @Autowired
    private lateinit var postIdGenerator: PostIdGenerator

    @Autowired
    private lateinit var postReviewIdGenerator: PostReviewIdGenerator

    @Autowired
    private lateinit var postRevisionIdGenerator: PostRevisionIdGenerator

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenManager: JwtTokenManager

    @Autowired
    private lateinit var clockHolder: ClockHolder

    @Test
    fun `GET reviews-reviewId - 진행 중인 리뷰를 조회하면 200 OK를 반환해야 한다`() {
        // Given - 게시글과 리뷰 생성
        val post = createTestPost()
        val review = createTestReview(post, status = PostReviewStatus.IN_REVIEW)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/reviews/{reviewId}", review.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(review.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.postId").value(post.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("IN_REVIEW"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.startedAt").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.deadline").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.winningRevisionId").isEmpty)
            .andDo(
                documentWithResource(
                    "리뷰 조회 성공",
                    builder()
                        .tag("Review")
                        .summary("리뷰 조회")
                        .description(
                            """
                            리뷰 ID로 특정 리뷰를 조회합니다.
                            
                            리뷰가 성공적으로 조회되면 200 OK 상태 코드와 함께
                            리뷰의 상세 정보가 반환됩니다.
                            모든 상태(IN_REVIEW, COMPLETED, CANCELLED)의 리뷰를 조회할 수 있습니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("조회할 리뷰의 ID"),
                        )
                        .responseFields(
                            fieldWithPath("id")
                                .type(JsonFieldType.NUMBER)
                                .description("리뷰 ID"),
                            fieldWithPath("postId")
                                .type(JsonFieldType.NUMBER)
                                .description("게시글 ID"),
                            fieldWithPath("startedAt")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 시작 시간"),
                            fieldWithPath("deadline")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 마감 시간"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 상태 (IN_REVIEW, COMPLETED, CANCELLED)"),
                            fieldWithPath("winningRevisionId")
                                .type(JsonFieldType.NUMBER)
                                .description("선정된 수정본 ID (완료된 리뷰만 해당)")
                                .optional(),
                        )
                        .responseSchema(
                            schema(
                                "${PostReviewResponse::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET reviews-reviewId - 완료된 리뷰를 조회하면 winningRevisionId와 함께 반환해야 한다`() {
        // Given - 완료된 리뷰 생성
        val post = createTestPost()
        val revision = createTestRevision()
        val review =
            createTestReview(
                post = post,
                status = PostReviewStatus.COMPLETED,
                winningRevisionId = revision.id,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/reviews/{reviewId}", review.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(review.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("COMPLETED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.winningRevisionId").value(revision.id.value))
            .andDo(
                documentWithResource(
                    "완료된 리뷰 조회",
                    builder()
                        .tag("Review")
                        .summary("리뷰 조회 - 완료된 리뷰")
                        .description(
                            """
                            완료된 리뷰를 조회합니다.
                            
                            완료된 리뷰는 COMPLETED 상태이며,
                            선정된 수정본 ID(winningRevisionId)가 함께 반환됩니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("조회할 리뷰의 ID"),
                        )
                        .responseFields(
                            fieldWithPath("id")
                                .type(JsonFieldType.NUMBER)
                                .description("리뷰 ID"),
                            fieldWithPath("postId")
                                .type(JsonFieldType.NUMBER)
                                .description("게시글 ID"),
                            fieldWithPath("startedAt")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 시작 시간"),
                            fieldWithPath("deadline")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 마감 시간"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 상태 (COMPLETED)"),
                            fieldWithPath("winningRevisionId")
                                .type(JsonFieldType.NUMBER)
                                .description("선정된 수정본 ID"),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET reviews-reviewId - 취소된 리뷰를 조회할 수 있어야 한다`() {
        // Given - 취소된 리뷰 생성
        val post = createTestPost()
        val review =
            createTestReview(
                post = post,
                status = PostReviewStatus.CANCELLED,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/reviews/{reviewId}", review.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(review.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("CANCELLED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.winningRevisionId").isEmpty)
            .andDo(
                documentWithResource(
                    "취소된 리뷰 조회",
                    builder()
                        .tag("Review")
                        .summary("리뷰 조회 - 취소된 리뷰")
                        .description(
                            """
                            취소된 리뷰를 조회합니다.
                            
                            취소된 리뷰는 CANCELLED 상태이며,
                            수정본이 제출되지 않아 자동으로 취소된 경우입니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("조회할 리뷰의 ID"),
                        )
                        .responseFields(
                            fieldWithPath("id")
                                .type(JsonFieldType.NUMBER)
                                .description("리뷰 ID"),
                            fieldWithPath("postId")
                                .type(JsonFieldType.NUMBER)
                                .description("게시글 ID"),
                            fieldWithPath("startedAt")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 시작 시간"),
                            fieldWithPath("deadline")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 마감 시간"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 상태 (CANCELLED)"),
                            fieldWithPath("winningRevisionId")
                                .type(JsonFieldType.NULL)
                                .description("선정된 수정본 ID (null)"),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET reviews-reviewId - 존재하지 않는 리뷰를 조회하면 404 Not Found를 반환해야 한다`() {
        // Given - 존재하지 않는 리뷰 ID
        val nonExistentId = 9999999L

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/reviews/{reviewId}", nonExistentId)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REVIEW_NOT_FOUND"))
            .andDo(
                documentWithResource(
                    "존재하지 않는 리뷰 조회",
                    builder()
                        .tag("Review")
                        .summary("리뷰 조회 - 존재하지 않는 리뷰")
                        .description(
                            """
                            존재하지 않는 리뷰 ID로 조회를 시도하면 404 Not Found를 반환합니다.
                            
                            에러 코드: REVIEW_NOT_FOUND
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("조회할 리뷰의 ID"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET reviews-reviewId - 잘못된 형식의 ID로 조회하면 400 Bad Request를 반환해야 한다`() {
        // Given - 잘못된 형식의 ID
        val invalidId = "invalid-id"

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/reviews/{reviewId}", invalidId)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andDo(
                documentWithResource(
                    "잘못된 형식의 ID로 리뷰 조회",
                    builder()
                        .tag("Review")
                        .summary("리뷰 조회 - 잘못된 ID 형식")
                        .description(
                            """
                            숫자가 아닌 잘못된 형식의 ID로 조회를 시도하면 4xx 에러를 반환합니다.
                            
                            리뷰 ID는 숫자(Long) 형식이어야 합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("조회할 리뷰의 ID (숫자여야 함)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET reviews-reviewId - 음수 ID로 조회하면 400 Bad Request를 반환해야 한다`() {
        // Given - 음수 ID
        val negativeId = -1L

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/reviews/{reviewId}", negativeId)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").exists())
            .andDo(
                documentWithResource(
                    "음수 ID로 리뷰 조회",
                    builder()
                        .tag("Review")
                        .summary("리뷰 조회 - 음수 ID")
                        .description(
                            """
                            음수 ID로 조회를 시도하면 400 Bad Request를 반환합니다.
                            
                            리뷰 ID는 양수여야 합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("조회할 리뷰의 ID (양수여야 함)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET reviews-reviewId - 인증 없이도 리뷰를 조회할 수 있어야 한다`() {
        // Given - 리뷰 생성 (인증 토큰 없이 조회)
        val post = createTestPost()
        val review = createTestReview(post)

        // When & Then - 인증 헤더 없이 요청
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/reviews/{reviewId}", review.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(review.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.postId").value(post.id.value))
            .andDo(
                documentWithResource(
                    "인증 없이 리뷰 조회",
                    builder()
                        .tag("Review")
                        .summary("리뷰 조회 - 인증 불필요")
                        .description(
                            """
                            리뷰 조회는 인증이 필요하지 않습니다.
                            
                            누구나 리뷰를 조회할 수 있으며,
                            Authorization 헤더 없이도 요청이 가능합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("조회할 리뷰의 ID"),
                        )
                        .responseFields(
                            fieldWithPath("id")
                                .type(JsonFieldType.NUMBER)
                                .description("리뷰 ID"),
                            fieldWithPath("postId")
                                .type(JsonFieldType.NUMBER)
                                .description("게시글 ID"),
                            fieldWithPath("startedAt")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 시작 시간"),
                            fieldWithPath("deadline")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 마감 시간"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 상태"),
                            fieldWithPath("winningRevisionId")
                                .type(JsonFieldType.NUMBER)
                                .description("선정된 수정본 ID")
                                .optional(),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET reviews-reviewId - 인증된 사용자도 리뷰를 조회할 수 있어야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser()
        val token = jwtTokenManager.generateAccessToken(user.id).token

        val post = createTestPost()
        val review = createTestReview(post)

        // When & Then - 인증 헤더와 함께 요청
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/reviews/{reviewId}", review.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(review.id.value))
            .andDo(
                documentWithResource(
                    "인증된 사용자의 리뷰 조회",
                    builder()
                        .tag("Review")
                        .summary("리뷰 조회 - 인증된 사용자")
                        .description(
                            """
                            인증된 사용자도 리뷰를 조회할 수 있습니다.
                            
                            Authorization 헤더에 유효한 JWT 토큰을 포함하여
                            요청할 수 있으며, 인증 여부와 관계없이
                            동일한 응답을 받습니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰} (선택사항)")
                                .optional(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("조회할 리뷰의 ID"),
                        )
                        .responseFields(
                            fieldWithPath("id")
                                .type(JsonFieldType.NUMBER)
                                .description("리뷰 ID"),
                            fieldWithPath("postId")
                                .type(JsonFieldType.NUMBER)
                                .description("게시글 ID"),
                            fieldWithPath("startedAt")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 시작 시간"),
                            fieldWithPath("deadline")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 마감 시간"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 상태"),
                            fieldWithPath("winningRevisionId")
                                .type(JsonFieldType.NUMBER)
                                .description("선정된 수정본 ID")
                                .optional(),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET reviews-reviewId - 리뷰의 deadline이 정확하게 반환되어야 한다`() {
        // Given - 특정 시간에 생성된 리뷰
        val now = clockHolder.now()
        val deadline = now.plusSeconds(259200) // 72시간 후

        val post = createTestPost()
        val review =
            PostReview(
                id = postReviewIdGenerator.generate(),
                postId = post.id,
                startedAt = now,
                deadline = deadline,
                status = PostReviewStatus.IN_REVIEW,
                winningRevisionId = null,
                startedBy = null,
            )
        postReviewRepository.save(review)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/reviews/{reviewId}", review.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.startedAt").value(now.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.deadline").value(deadline.toString()))
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
        post: Post,
        status: PostReviewStatus = PostReviewStatus.IN_REVIEW,
        winningRevisionId: PostRevisionId? = null,
        startedBy: Long? = null,
    ): PostReview {
        val now = clockHolder.now()
        val review =
            PostReview(
                id = postReviewIdGenerator.generate(),
                postId = post.id,
                startedAt = now,
                // 72시간 후
                deadline = now.plusSeconds(259200),
                status = status,
                winningRevisionId = winningRevisionId,
                startedBy = startedBy,
            )

        return postReviewRepository.save(review)
    }

    /**
     * 테스트용 수정본 생성 헬퍼 메서드
     */
    private fun createTestRevision(): PostRevision {
        val now = clockHolder.now()
        val post = createTestPost()
        val review = createTestReview(post)

        val revision =
            PostRevision(
                id = postRevisionIdGenerator.generate(),
                reviewId = review.id,
                title = PostTitle("수정된 제목"),
                body = PostBody("수정된 본문 내용입니다. 이 내용은 리뷰를 통해 개선되었습니다."),
                authorId = 1L,
                submittedAt = now,
                voteCount = 5,
            )

        return postRevisionRepository.save(revision)
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
}
