package me.helloc.techwikiplus.post.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters.Companion.builder
import com.epages.restdocs.apispec.Schema.Companion.schema
import me.helloc.techwikiplus.common.config.BaseE2eTest
import me.helloc.techwikiplus.common.config.annotations.E2eTest
import me.helloc.techwikiplus.common.config.documentation.withStandardErrorResponse
import me.helloc.techwikiplus.common.infrastructure.security.jwt.JwtTokenManager
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostBody
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
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionRepository
import me.helloc.techwikiplus.post.dto.request.PostRevisionRequest
import me.helloc.techwikiplus.post.dto.request.ReviewCommentRequest
import me.helloc.techwikiplus.post.dto.response.PostRevisionResponse
import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.user.domain.model.Nickname
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole
import me.helloc.techwikiplus.user.domain.model.UserStatus
import me.helloc.techwikiplus.user.domain.service.port.UserRepository
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
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
 * PostRevisionController E2E 테스트
 *
 * - 전체 애플리케이션 컨텍스트 로드
 * - TestContainers를 통한 실제 DB 연동
 * - 운영 환경과 동일한 설정
 * - End-to-End 검증
 * - API 문서 자동 생성 (generateDocs = true)
 */
@E2eTest(generateDocs = true)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-post",
        "spring.application.version=1.0.0-INTEGRATION",
        "api.documentation.enabled=true",
    ],
)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PostRevisionControllerE2eTest : BaseE2eTest() {
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
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenManager: JwtTokenManager

    @Test
    @Order(1)
    fun `POST revisions - 로그인한 사용자가 검수 중인 게시글에 개정안을 제출할 수 있다`() {
        // Given - 게시글과 검수 생성
        val post = createTestPost()
        val review = createTestReview(post)

        // 일반 사용자 생성 및 로그인
        val user = createTestUser(role = UserRole.USER)
        val userToken = jwtTokenManager.generateAccessToken(user.id).token

        val request =
            PostRevisionRequest(
                title = "개선된 게시글 제목",
                body = "이것은 개선된 게시글의 본문 내용입니다. 원래 내용보다 더 명확하고 상세한 설명을 포함하고 있습니다.",
                reviewComments = emptyList(),
            )

        // When & Then
        var locationHeader: String? = null

        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.LOCATION))
            .andDo { result ->
                locationHeader = result.response.getHeader(HttpHeaders.LOCATION)
            }
            .andDo(
                documentWithResource(
                    "개정안 제출 - 로그인 사용자",
                    builder()
                        .tag("Post Revision")
                        .summary("개정안 제출")
                        .description(
                            """
                            검수 중인 게시글에 대한 개정안을 제출합니다.
                            
                            개정안 제출 시:
                            - 검수가 IN_REVIEW 상태여야 합니다
                            - 로그인한 사용자와 비로그인 사용자 모두 제출 가능합니다
                            - 제출된 개정안은 투표를 받을 수 있습니다
                            - 여러 개정안이 동일 검수에 제출될 수 있습니다
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰} (선택사항)").optional(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("개정안을 제출할 검수 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("개정안 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("개정안 본문"),
                            fieldWithPath("reviewComments")
                                .type(JsonFieldType.ARRAY)
                                .description("리뷰 댓글 목록 (선택사항)").optional(),
                        )
                        .responseHeaders(
                            headerWithName(HttpHeaders.LOCATION)
                                .description("생성된 개정안의 URI"),
                        )
                        .requestSchema(
                            schema(
                                "${PostRevisionRequest::class.simpleName}",
                            ),
                        )
                        .responseSchema(
                            schema(
                                "${PostRevisionResponse::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )

        // Then - Location 헤더 확인
        locationHeader shouldNotBe null
        locationHeader!! shouldContain "/revisions/"

        // DB에 개정안이 저장되었는지 확인
        val revisionId = locationHeader!!.substringAfterLast("/").toLongOrNull()
        revisionId shouldNotBe null

        val savedRevision = postRevisionRepository.findById(PostRevisionId(revisionId!!))
        savedRevision shouldNotBe null
        savedRevision!!.reviewId.value shouldBe review.id.value
        savedRevision.authorId shouldBe user.id.value
        savedRevision.title.value shouldBe "개선된 게시글 제목"
        savedRevision.body.value shouldContain "개선된 게시글의 본문"
    }

    @Test
    @Order(2)
    fun `POST revisions - 비로그인 사용자도 개정안을 제출할 수 있다`() {
        // Given - 게시글과 검수 생성
        val post = createTestPost()
        val review = createTestReview(post)

        val request =
            PostRevisionRequest(
                title = "익명 사용자의 개정안",
                body = "비로그인 사용자가 제출한 개정안입니다. 최소 30자 이상의 내용을 포함하고 있습니다.",
                reviewComments = emptyList(),
            )

        // When & Then - 인증 헤더 없이 요청
        var locationHeader: String? = null

        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.LOCATION))
            .andDo { result ->
                locationHeader = result.response.getHeader(HttpHeaders.LOCATION)
            }
            .andDo(
                documentWithResource(
                    "개정안 제출 - 비로그인 사용자",
                    builder()
                        .tag("Post Revision")
                        .summary("개정안 제출 (비로그인)")
                        .description(
                            """
                            비로그인 사용자도 개정안을 제출할 수 있습니다.
                            
                            인증 헤더 없이 요청해도 개정안이 정상적으로 제출되며,
                            authorId 필드는 null로 저장됩니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("개정안을 제출할 검수 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("개정안 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("개정안 본문"),
                            fieldWithPath("reviewComments")
                                .type(JsonFieldType.ARRAY)
                                .description("리뷰 댓글 목록 (선택사항)").optional(),
                        )
                        .requestSchema(
                            schema(
                                "${PostRevisionRequest::class.simpleName}",
                            ),
                        )
                        .responseHeaders(
                            headerWithName(HttpHeaders.LOCATION)
                                .description("생성된 개정안의 URI"),
                        )
                        .build(),
                ),
            )

        // Then - DB 확인
        val revisionId = locationHeader!!.substringAfterLast("/").toLongOrNull()
        revisionId shouldNotBe null

        val savedRevision = postRevisionRepository.findById(PostRevisionId(revisionId!!))
        savedRevision shouldNotBe null
        savedRevision!!.authorId shouldBe null // 비로그인 사용자이므로 null
        savedRevision.title.value shouldBe "익명 사용자의 개정안"
    }

    @Test
    @Order(3)
    fun `POST revisions - 동일한 검수에 여러 개정안을 제출할 수 있다`() {
        // Given - 게시글과 검수 생성
        val post = createTestPost()
        val review = createTestReview(post)

        val user1 = createTestUser(nickname = "User1")
        val user2 = createTestUser(nickname = "User2")
        val token1 = jwtTokenManager.generateAccessToken(user1.id).token
        val token2 = jwtTokenManager.generateAccessToken(user2.id).token

        // When - 첫 번째 개정안 제출
        val request1 =
            PostRevisionRequest(
                title = "첫 번째 개정안",
                body = "첫 번째 사용자가 제출한 개정안입니다. 최소 30자 이상의 내용을 포함하고 있습니다.",
                reviewComments = emptyList(),
            )

        var locationHeader1: String? = null
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andDo { result ->
                locationHeader1 = result.response.getHeader(HttpHeaders.LOCATION)
            }

        // 두 번째 개정안 제출
        val request2 =
            PostRevisionRequest(
                title = "두 번째 개정안",
                body = "두 번째 사용자가 제출한 개정안입니다. 첫 번째와는 다른 관점의 개선안을 제시합니다.",
                reviewComments = emptyList(),
            )

        var locationHeader2: String? = null
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andDo { result ->
                locationHeader2 = result.response.getHeader(HttpHeaders.LOCATION)
            }
            .andDo(
                documentWithResource(
                    "개정안 제출 - 다중 개정안",
                    builder()
                        .tag("Post Revision")
                        .summary("동일 검수에 다중 개정안 제출")
                        .description(
                            """
                            하나의 검수에 여러 사용자가 각각 개정안을 제출할 수 있습니다.
                            
                            모든 개정안은 독립적으로 관리되며, 각각 투표를 받을 수 있습니다.
                            가장 많은 투표를 받은 개정안이 최종적으로 선택될 수 있습니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("개정안을 제출할 검수 ID"),
                        )
                        .requestSchema(
                            schema(
                                "${PostRevisionRequest::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )

        // Then - 두 개정안이 모두 저장되었는지 확인
        val revisionId1 = locationHeader1!!.substringAfterLast("/").toLong()
        val revisionId2 = locationHeader2!!.substringAfterLast("/").toLong()

        revisionId1 shouldNotBe revisionId2 // 서로 다른 ID

        val revisions = postRevisionRepository.findByReviewId(review.id)
        revisions.size shouldBe 2
        revisions.any { it.authorId == user1.id.value } shouldBe true
        revisions.any { it.authorId == user2.id.value } shouldBe true
    }

    @Test
    @Order(10)
    fun `POST revisions - 제목이 비어있으면 400 Bad Request를 반환한다`() {
        // Given
        val post = createTestPost()
        val review = createTestReview(post)

        val request =
            PostRevisionRequest(
                // 빈 제목
                title = "",
                body = "본문은 충분히 긴 내용입니다. 최소 30자 이상의 내용을 포함하고 있습니다.",
                reviewComments = emptyList(),
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("BLANK_TITLE"))
            .andDo(
                documentWithResource(
                    "개정안 제출 실패 - 빈 제목",
                    builder()
                        .tag("Post Revision")
                        .summary("개정안 제출 - 제목 유효성 검사 실패")
                        .description(
                            """
                            제목이 비어있거나 너무 긴 경우 400 Bad Request를 반환합니다.
                            
                            제목 제약 사항:
                            - 빈 문자열 불가
                            - 최대 150자 이하
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("개정안을 제출할 검수 ID"),
                        )
                        .requestSchema(
                            schema(
                                "${PostRevisionRequest::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    @Order(11)
    fun `POST revisions - 본문이 너무 짧으면 400 Bad Request를 반환한다`() {
        // Given
        val post = createTestPost()
        val review = createTestReview(post)

        val request =
            PostRevisionRequest(
                title = "정상적인 제목입니다",
                // 너무 짧은 본문
                body = "너무 짧은 본문",
                reviewComments = emptyList(),
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("CONTENT_TOO_SHORT"))
            .andDo(
                documentWithResource(
                    "개정안 제출 실패 - 본문 길이 부족",
                    builder()
                        .tag("Post Revision")
                        .summary("개정안 제출 - 본문 유효성 검사 실패")
                        .description(
                            """
                            본문이 너무 짧거나 긴 경우 400 Bad Request를 반환합니다.
                            
                            본문 제약 사항:
                            - 최소 30자 이상
                            - 최대 50000자 이하
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("개정안을 제출할 검수 ID"),
                        )
                        .requestSchema(
                            schema(
                                "${PostRevisionRequest::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    @Order(12)
    fun `POST revisions - 잘못된 형식의 검수 ID로 요청하면 400 Bad Request를 반환한다`() {
        // Given
        val invalidReviewId = "invalid-id"

        val request =
            PostRevisionRequest(
                title = "정상적인 제목입니다",
                body = "정상적인 본문 내용입니다. 최소 30자 이상의 내용을 포함하고 있습니다.",
                reviewComments = emptyList(),
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", invalidReviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andDo(
                documentWithResource(
                    "개정안 제출 실패 - 잘못된 검수 ID 형식",
                    builder()
                        .tag("Post Revision")
                        .summary("개정안 제출 - 잘못된 ID 형식")
                        .description(
                            """
                            검수 ID가 숫자 형식이 아닌 경우
                            400 Bad Request 에러를 반환합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("개정안을 제출할 검수 ID"),
                        )
                        .requestSchema(
                            schema(
                                "${PostRevisionRequest::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    @Order(4)
    fun `POST revisions - 만료된 토큰으로 요청 시에도 개정안을 제출할 수 있다`() {
        // Given
        val post = createTestPost()
        val review = createTestReview(post)
        val expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.invalid"

        val request =
            PostRevisionRequest(
                title = "만료된 토큰으로 제출한 개정안",
                body = "만료된 토큰이 있어도 개정안을 제출할 수 있습니다. 이 API는 공개 API입니다.",
                reviewComments = emptyList(),
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $expiredToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.LOCATION))
            .andDo(
                documentWithResource(
                    "개정안 제출 - 만료된 토큰",
                    builder()
                        .tag("Post Revision")
                        .summary("개정안 제출 - 토큰 무시")
                        .description(
                            """
                            개정안 제출 API는 공개 API이므로 만료되거나 유효하지 않은 토큰이 있어도
                            개정안을 제출할 수 있습니다. 이 경우 비로그인 사용자로 처리됩니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {만료된 JWT 토큰}").optional(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("개정안을 제출할 검수 ID"),
                        )
                        .requestSchema(
                            schema(
                                "${PostRevisionRequest::class.simpleName}",
                            ),
                        )
                        .responseHeaders(
                            headerWithName(HttpHeaders.LOCATION)
                                .description("생성된 개정안의 URI"),
                        )
                        .build(),
                ),
            )
    }

    @Test
    @Order(5)
    fun `POST revisions - 단일 리뷰 댓글과 함께 개정안을 제출할 수 있다`() {
        // Given
        val post = createTestPost()
        val review = createTestReview(post)

        val user = createTestUser(role = UserRole.USER)
        val userToken = jwtTokenManager.generateAccessToken(user.id).token

        val request =
            PostRevisionRequest(
                title = "리뷰 댓글이 포함된 개정안",
                body = "이 개정안은 리뷰 댓글과 함께 제출됩니다. 최소 30자 이상의 내용을 포함하고 있습니다.",
                reviewComments =
                    listOf(
                        ReviewCommentRequest(
                            lineNumber = 10,
                            comment = "이 부분의 설명이 부정확합니다. 올바른 정보로 수정이 필요합니다.",
                            type = "INACCURACY",
                        ),
                    ),
            )

        // When & Then
        var locationHeader: String? = null

        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.LOCATION))
            .andDo { result ->
                locationHeader = result.response.getHeader(HttpHeaders.LOCATION)
            }
            .andDo(
                documentWithResource(
                    "개정안 제출 - 리뷰 댓글 포함",
                    builder()
                        .tag("Post Revision")
                        .summary("리뷰 댓글과 함께 개정안 제출")
                        .description(
                            """
                            개정안에 리뷰 댓글을 포함하여 제출합니다.

                            리뷰 댓글 타입:
                            - INACCURACY: 부정확한 내용 지적
                            - NEEDS_IMPROVEMENT: 개선이 필요한 부분 제안
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰} (선택사항)").optional(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("개정안을 제출할 검수 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("개정안 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("개정안 본문"),
                            fieldWithPath("reviewComments")
                                .type(JsonFieldType.ARRAY)
                                .description("리뷰 댓글 목록").optional(),
                            fieldWithPath("reviewComments[].lineNumber")
                                .type(JsonFieldType.NUMBER)
                                .description("댓글이 참조하는 라인 번호"),
                            fieldWithPath("reviewComments[].comment")
                                .type(JsonFieldType.STRING)
                                .description("댓글 내용"),
                            fieldWithPath("reviewComments[].type")
                                .type(JsonFieldType.STRING)
                                .description("댓글 타입 (INACCURACY 또는 NEEDS_IMPROVEMENT)"),
                        )
                        .responseHeaders(
                            headerWithName(HttpHeaders.LOCATION)
                                .description("생성된 개정안의 URI"),
                        )
                        .requestSchema(
                            schema(
                                "${PostRevisionRequest::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )

        // Then - DB 확인
        val revisionId = locationHeader!!.substringAfterLast("/").toLong()
        val savedRevision = postRevisionRepository.findById(PostRevisionId(revisionId))

        savedRevision shouldNotBe null
        savedRevision!!.reviewComments.size shouldBe 1
        savedRevision.reviewComments[0].lineNumber shouldBe 10
        savedRevision.reviewComments[0].comment shouldContain "부정확합니다"
        savedRevision.reviewComments[0].type.name shouldBe "INACCURACY"
    }

    @Test
    @Order(6)
    fun `POST revisions - 여러 리뷰 댓글과 함께 개정안을 제출할 수 있다`() {
        // Given
        val post = createTestPost()
        val review = createTestReview(post)

        val request =
            PostRevisionRequest(
                title = "여러 리뷰 댓글이 포함된 개정안",
                body = "이 개정안은 여러 개의 리뷰 댓글과 함께 제출됩니다. 다양한 타입의 피드백을 포함합니다.",
                reviewComments =
                    listOf(
                        ReviewCommentRequest(
                            lineNumber = 5,
                            comment = "이 부분의 정보가 오래되었습니다.",
                            type = "INACCURACY",
                        ),
                        ReviewCommentRequest(
                            lineNumber = 15,
                            comment = "코드 예제를 추가하면 더 명확할 것 같습니다.",
                            type = "NEEDS_IMPROVEMENT",
                        ),
                        ReviewCommentRequest(
                            lineNumber = 25,
                            comment = "용어 설명이 부족합니다.",
                            type = "NEEDS_IMPROVEMENT",
                        ),
                    ),
            )

        // When
        var locationHeader: String? = null

        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andDo { result ->
                locationHeader = result.response.getHeader(HttpHeaders.LOCATION)
            }

        // Then
        val revisionId = locationHeader!!.substringAfterLast("/").toLong()
        val savedRevision = postRevisionRepository.findById(PostRevisionId(revisionId))

        savedRevision shouldNotBe null
        savedRevision!!.reviewComments.size shouldBe 3

        // 댓글들이 올바르게 저장되었는지 확인
        val comments = savedRevision.reviewComments.sortedBy { it.lineNumber }
        comments[0].lineNumber shouldBe 5
        comments[0].type.name shouldBe "INACCURACY"
        comments[1].lineNumber shouldBe 15
        comments[1].type.name shouldBe "NEEDS_IMPROVEMENT"
        comments[2].lineNumber shouldBe 25
        comments[2].type.name shouldBe "NEEDS_IMPROVEMENT"
    }

    @Test
    @Order(7)
    fun `POST revisions - 리뷰 댓글 내용이 비어있으면 400 Bad Request를 반환한다`() {
        // Given
        val post = createTestPost()
        val review = createTestReview(post)

        val request =
            PostRevisionRequest(
                title = "리뷰 댓글 검증 테스트",
                body = "이 개정안은 빈 리뷰 댓글을 포함하고 있어 실패해야 합니다. 최소 30자 이상의 내용입니다.",
                reviewComments =
                    listOf(
                        ReviewCommentRequest(
                            lineNumber = 10,
                            // 빈 댓글
                            comment = "",
                            type = "INACCURACY",
                        ),
                    ),
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("BLANK_REVIEW_COMMENT"))
            .andDo(
                documentWithResource(
                    "개정안 제출 실패 - 빈 리뷰 댓글",
                    builder()
                        .tag("Post Revision")
                        .summary("개정안 제출 - 리뷰 댓글 유효성 검사 실패")
                        .description(
                            """
                            리뷰 댓글 내용이 비어있는 경우 400 Bad Request를 반환합니다.

                            리뷰 댓글 제약 사항:
                            - 빈 문자열 불가
                            - 최대 15000자 이하
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("개정안을 제출할 검수 ID"),
                        )
                        .requestSchema(
                            schema(
                                "${PostRevisionRequest::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    @Order(8)
    fun `POST revisions - 리뷰 댓글이 너무 길면 400 Bad Request를 반환한다`() {
        // Given
        val post = createTestPost()
        val review = createTestReview(post)

        val veryLongComment = "a".repeat(15001) // 15000자 초과

        val request =
            PostRevisionRequest(
                title = "리뷰 댓글 길이 검증 테스트",
                body = "이 개정안은 너무 긴 리뷰 댓글을 포함하고 있어 실패해야 합니다. 최소 30자 이상입니다.",
                reviewComments =
                    listOf(
                        ReviewCommentRequest(
                            lineNumber = 10,
                            comment = veryLongComment,
                            type = "NEEDS_IMPROVEMENT",
                        ),
                    ),
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REVIEW_COMMENT_TOO_LONG"))
    }

    @Test
    @Order(9)
    fun `POST revisions - 리뷰 댓글의 라인 번호가 유효하지 않으면 400 Bad Request를 반환한다`() {
        // Given
        val post = createTestPost()
        val review = createTestReview(post)

        val request =
            PostRevisionRequest(
                title = "라인 번호 검증 테스트",
                body = "이 개정안은 잘못된 라인 번호를 가진 댓글을 포함합니다. 최소 30자 이상의 내용입니다.",
                reviewComments =
                    listOf(
                        ReviewCommentRequest(
                            // 잘못된 라인 번호
                            lineNumber = 0,
                            comment = "이 댓글은 잘못된 라인 번호를 가지고 있습니다.",
                            type = "INACCURACY",
                        ),
                    ),
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_LINE_NUMBER"))
            .andDo(
                documentWithResource(
                    "개정안 제출 실패 - 잘못된 라인 번호",
                    builder()
                        .tag("Post Revision")
                        .summary("개정안 제출 - 라인 번호 유효성 검사 실패")
                        .description(
                            """
                            리뷰 댓글의 라인 번호가 0 이하인 경우 400 Bad Request를 반환합니다.

                            라인 번호는 1 이상의 양수여야 합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("reviewId")
                                .description("개정안을 제출할 검수 ID"),
                        )
                        .requestSchema(
                            schema(
                                "${PostRevisionRequest::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    @Order(10)
    fun `POST revisions - 리뷰 댓글 타입이 유효하지 않으면 400 Bad Request를 반환한다`() {
        // Given
        val post = createTestPost()
        val review = createTestReview(post)

        // JSON 문자열을 직접 만들어서 잘못된 타입 전송
        val invalidRequest =
            """
            {
                "title": "잘못된 타입 테스트",
                "body": "이 개정안은 잘못된 리뷰 댓글 타입을 포함합니다. 최소 30자 이상의 내용입니다.",
                "reviewComments": [
                    {
                        "lineNumber": 10,
                        "comment": "이 댓글은 잘못된 타입을 가지고 있습니다.",
                        "type": "INVALID_TYPE"
                    }
                ]
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @Order(11)
    fun `POST revisions - 리뷰 댓글이 있는 개정안과 없는 개정안을 모두 제출할 수 있다`() {
        // Given
        val post = createTestPost()
        val review = createTestReview(post)

        // 첫 번째: 리뷰 댓글 없는 개정안
        val requestWithoutComments =
            PostRevisionRequest(
                title = "리뷰 댓글 없는 개정안",
                body = "이 개정안은 리뷰 댓글 없이 제출됩니다. 최소 30자 이상의 내용을 포함하고 있습니다.",
                reviewComments = emptyList(),
            )

        var locationHeader1: String? = null
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithoutComments)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andDo { result ->
                locationHeader1 = result.response.getHeader(HttpHeaders.LOCATION)
            }

        // 두 번째: 리뷰 댓글 있는 개정안
        val requestWithComments =
            PostRevisionRequest(
                title = "리뷰 댓글 있는 개정안",
                body = "이 개정안은 리뷰 댓글과 함께 제출됩니다. 최소 30자 이상의 내용을 포함하고 있습니다.",
                reviewComments =
                    listOf(
                        ReviewCommentRequest(
                            lineNumber = 5,
                            comment = "개선이 필요한 부분입니다.",
                            type = "NEEDS_IMPROVEMENT",
                        ),
                    ),
            )

        var locationHeader2: String? = null
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithComments)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andDo { result ->
                locationHeader2 = result.response.getHeader(HttpHeaders.LOCATION)
            }

        // Then - 두 개정안 모두 저장 확인
        val revisionId1 = locationHeader1!!.substringAfterLast("/").toLong()
        val revisionId2 = locationHeader2!!.substringAfterLast("/").toLong()

        val revision1 = postRevisionRepository.findById(PostRevisionId(revisionId1))
        val revision2 = postRevisionRepository.findById(PostRevisionId(revisionId2))

        revision1 shouldNotBe null
        revision1!!.reviewComments.size shouldBe 0

        revision2 shouldNotBe null
        revision2!!.reviewComments.size shouldBe 1
    }

    @Test
    @Order(12)
    fun `POST revisions - 개정안 제출 시 투표 수는 0으로 초기화된다`() {
        // Given
        val post = createTestPost()
        val review = createTestReview(post)

        val request =
            PostRevisionRequest(
                title = "투표 수 확인용 개정안",
                body = "이 개정안의 초기 투표 수는 0이어야 합니다. 최소 30자 이상의 내용을 포함합니다.",
                reviewComments = emptyList(),
            )

        // When
        var locationHeader: String? = null
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/reviews/{reviewId}/revisions", review.id.value)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andDo { result ->
                locationHeader = result.response.getHeader(HttpHeaders.LOCATION)
            }

        // Then
        val revisionId = locationHeader!!.substringAfterLast("/").toLong()
        val savedRevision = postRevisionRepository.findById(PostRevisionId(revisionId))

        savedRevision shouldNotBe null
        savedRevision!!.voteCount shouldBe 0
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
     * 테스트용 검수 생성 헬퍼 메서드
     */
    private fun createTestReview(
        post: Post,
        status: PostReviewStatus = PostReviewStatus.IN_REVIEW,
        startedBy: Long? = null,
    ): PostReview {
        val now = Instant.now()
        val reviewId = postReviewIdGenerator.generate()
        val review =
            PostReview(
                id = reviewId,
                postId = post.id,
                startedAt = now,
                // 72시간
                deadline = now.plusSeconds(259200),
                status = status,
                startedBy = startedBy,
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
