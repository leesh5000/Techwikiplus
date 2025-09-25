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
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostReviewStatus
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
 * PostReviewController E2E 테스트
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
class PostReviewControllerE2eTest : BaseE2eTest() {
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
    fun `POST reviews - 로그인한 사용자가 게시글 검수를 시작할 수 있다`() {
        // Given - 게시글 생성
        val post = createTestPost()

        // 일반 사용자 생성 및 로그인
        val user = createTestUser(role = UserRole.USER)
        val userToken = jwtTokenManager.generateAccessToken(user.id).token

        // When & Then
        var locationHeader: String? = null

        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/reviews", post.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $userToken")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.LOCATION))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.postId").value(post.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.startedAt").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.deadline").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("IN_REVIEW"))
            .andDo { result ->
                locationHeader = result.response.getHeader(HttpHeaders.LOCATION)
            }
            .andDo(
                documentWithResource(
                    "게시글 검수 시작 - 로그인 사용자",
                    builder()
                        .tag("Post Review")
                        .summary("게시글 검수 시작")
                        .description(
                            """
                            게시글에 대한 검수를 시작합니다.
                            
                            검수가 시작되면:
                            - 게시글이 IN_REVIEW 상태가 됩니다
                            - 72시간의 검수 기한이 설정됩니다
                            - 이미 검수 중인 게시글은 다시 검수를 시작할 수 없습니다
                            - 로그인한 사용자와 비로그인 사용자 모두 검수를 시작할 수 있습니다
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰} (선택사항)").optional(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("검수를 시작할 게시글 ID"),
                        )
                        .responseHeaders(
                            headerWithName(HttpHeaders.LOCATION)
                                .description("생성된 검수의 URI"),
                        )
                        .responseFields(
                            fieldWithPath("id")
                                .type(JsonFieldType.NUMBER)
                                .description("검수 ID"),
                            fieldWithPath("postId")
                                .type(JsonFieldType.NUMBER)
                                .description("게시글 ID"),
                            fieldWithPath("startedAt")
                                .type(JsonFieldType.STRING)
                                .description("검수 시작 시간 (ISO-8601)"),
                            fieldWithPath("deadline")
                                .type(JsonFieldType.STRING)
                                .description("검수 마감 시간 (ISO-8601)"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("검수 상태 (IN_REVIEW)"),
                            fieldWithPath("winningRevisionId")
                                .type(JsonFieldType.NULL)
                                .description("승리한 개정안 ID (검수 중일 때는 null)").optional(),
                        )
                        .responseSchema(
                            schema(
                                "${PostReviewResponse::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )

        // Then - Location 헤더 확인
        locationHeader shouldNotBe null
        locationHeader!! shouldContain "/reviews/"

        // DB에 검수가 저장되었는지 확인
        val reviewId = locationHeader!!.substringAfterLast("/").toLongOrNull()
        reviewId shouldNotBe null

        val savedReview = postReviewRepository.findById(PostReviewId(reviewId!!))
        savedReview shouldNotBe null
        savedReview!!.postId.value shouldBe post.id.value
        savedReview.status shouldBe PostReviewStatus.IN_REVIEW
        savedReview.startedBy shouldBe user.id.value

        // 게시글 상태가 IN_REVIEW로 변경되었는지 확인
        val updatedPost = postRepository.findBy(post.id)
        updatedPost shouldNotBe null
        updatedPost!!.status shouldBe PostStatus.IN_REVIEW
    }

    @Test
    fun `POST reviews - 비로그인 사용자도 게시글 검수를 시작할 수 있다`() {
        // Given - 게시글 생성
        val post = createTestPost()

        // When & Then - 인증 헤더 없이 요청
        var locationHeader: String? = null

        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/reviews", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.LOCATION))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.postId").value(post.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("IN_REVIEW"))
            .andDo { result ->
                locationHeader = result.response.getHeader(HttpHeaders.LOCATION)
            }
            .andDo(
                documentWithResource(
                    "게시글 검수 시작 - 비로그인 사용자",
                    builder()
                        .tag("Post Review")
                        .summary("게시글 검수 시작 (비로그인)")
                        .description(
                            """
                            비로그인 사용자도 게시글 검수를 시작할 수 있습니다.
                            
                            인증 헤더 없이 요청해도 검수가 정상적으로 시작되며,
                            startedBy 필드는 null로 저장됩니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("검수를 시작할 게시글 ID"),
                        )
                        .responseHeaders(
                            headerWithName(HttpHeaders.LOCATION)
                                .description("생성된 검수의 URI"),
                        )
                        .responseFields(
                            fieldWithPath("id")
                                .type(JsonFieldType.NUMBER)
                                .description("검수 ID"),
                            fieldWithPath("postId")
                                .type(JsonFieldType.NUMBER)
                                .description("게시글 ID"),
                            fieldWithPath("startedAt")
                                .type(JsonFieldType.STRING)
                                .description("검수 시작 시간"),
                            fieldWithPath("deadline")
                                .type(JsonFieldType.STRING)
                                .description("검수 마감 시간"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("검수 상태"),
                            fieldWithPath("winningRevisionId")
                                .type(JsonFieldType.NULL)
                                .description("승리한 개숡안 ID").optional(),
                        )
                        .responseSchema(
                            schema(
                                "${PostReviewResponse::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )

        // Then - DB 확인
        val reviewId = locationHeader!!.substringAfterLast("/").toLongOrNull()
        reviewId shouldNotBe null

        val savedReview = postReviewRepository.findById(PostReviewId(reviewId!!))
        savedReview shouldNotBe null
        savedReview!!.startedBy shouldBe null // 비로그인 사용자이므로 null
    }

    @Test
    fun `POST reviews - 이미 검수 중인 게시글은 다시 검수를 시작할 수 없다`() {
        // Given - 게시글 생성 및 첫 번째 검수 시작
        val post = createTestPost()

        // 첫 번째 검수 시작
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/reviews", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)

        // When & Then - 두 번째 검수 시도
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/reviews", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isConflict)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"))
            .andDo(
                documentWithResource(
                    "게시글 검수 시작 실패 - 이미 검수 중",
                    builder()
                        .tag("Post Review")
                        .summary("게시글 검수 시작 - 중복 검수")
                        .description(
                            """
                            이미 검수가 진행 중인 게시글은 다시 검수를 시작할 수 없습니다.
                            
                            409 Conflict 상태 코드와 함께 REVIEW_ALREADY_EXISTS 에러를 반환합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("검수를 시작할 게시글 ID"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST reviews - 존재하지 않는 게시글은 검수를 시작할 수 없다`() {
        // Given - 존재하지 않는 게시글 ID
        val nonExistentPostId = 999999L

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/reviews", nonExistentPostId)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("POST_NOT_FOUND"))
            .andDo(
                documentWithResource(
                    "게시글 검수 시작 실패 - 게시글 없음",
                    builder()
                        .tag("Post Review")
                        .summary("게시글 검수 시작 - 게시글 없음")
                        .description(
                            """
                            존재하지 않는 게시글에 대해 검수를 시작하려고 하면
                            404 Not Found와 함께 POST_NOT_FOUND 에러를 반환합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("검수를 시작할 게시글 ID"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST reviews - 삭제된 게시글은 검수를 시작할 수 없다`() {
        // Given - 삭제된 게시글 생성
        val deletedPost = createTestPost(status = PostStatus.DELETED)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/reviews", deletedPost.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("POST_NOT_FOUND"))
            .andDo(
                documentWithResource(
                    "게시글 검수 시작 실패 - 삭제된 게시글",
                    builder()
                        .tag("Post Review")
                        .summary("게시글 검수 시작 - 삭제된 게시글")
                        .description(
                            """
                            삭제된 게시글(DELETED 상태)에 대해 검수를 시작하려고 하면
                            404 Not Found와 함께 POST_NOT_FOUND 에러를 반환합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("검수를 시작할 게시글 ID"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST reviews - 검수 시작 시 72시간의 deadline이 설정되어야 한다`() {
        // Given - 게시글 생성
        val post = createTestPost()
        val beforeRequest = Instant.now()

        // When
        var responseBody: String? = null
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/reviews", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andDo { result ->
                responseBody = result.response.contentAsString
            }

        // Then - 응답 파싱
        val response = objectMapper.readValue(responseBody, PostReviewResponse::class.java)
        val afterRequest = Instant.now()

        // 시작 시간 검증
        response.startedAt.isAfter(beforeRequest.minusSeconds(1)) shouldBe true
        response.startedAt.isBefore(afterRequest.plusSeconds(1)) shouldBe true

        // 마감 시간 검증 (72시간 = 259200초)
        val expectedDeadline = response.startedAt.plusSeconds(259200)
        response.deadline shouldBe expectedDeadline

        // DB 검증
        val savedReview = postReviewRepository.findById(PostReviewId(response.id))
        savedReview shouldNotBe null
        savedReview!!.deadline shouldBe response.deadline
    }

    @Test
    fun `POST reviews - 여러 게시글에 대해 독립적으로 검수를 시작할 수 있다`() {
        // Given - 여러 게시글 생성
        val post1 = createTestPost(title = "첫 번째 게시글")
        val post2 = createTestPost(title = "두 번째 게시글")
        val post3 = createTestPost(title = "세 번째 게시글")

        // When - 각 게시글에 대해 검수 시작
        val reviewIds = mutableListOf<Long>()

        listOf(post1, post2, post3).forEach { post ->
            var locationHeader: String? = null
            mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/reviews", post.id.value)
                    .accept(MediaType.APPLICATION_JSON),
            )
                .andExpect(MockMvcResultMatchers.status().isCreated)
                .andDo { result ->
                    locationHeader = result.response.getHeader(HttpHeaders.LOCATION)
                }

            val reviewId = locationHeader!!.substringAfterLast("/").toLong()
            reviewIds.add(reviewId)
        }

        // Then - 모든 검수가 독립적으로 생성되었는지 확인
        reviewIds.size shouldBe 3
        reviewIds.toSet().size shouldBe 3 // 모든 ID가 고유함

        // 각 검수가 올바른 게시글을 참조하는지 확인
        val review1 = postReviewRepository.findById(PostReviewId(reviewIds[0]))
        review1!!.postId.value shouldBe post1.id.value

        val review2 = postReviewRepository.findById(PostReviewId(reviewIds[1]))
        review2!!.postId.value shouldBe post2.id.value

        val review3 = postReviewRepository.findById(PostReviewId(reviewIds[2]))
        review3!!.postId.value shouldBe post3.id.value
    }

    @Test
    fun `POST reviews - 만료된 토큰으로 요청 시에도 검수를 시작할 수 있다`() {
        // Given - 게시글 생성 및 만료된 토큰
        val post = createTestPost()
        val expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.invalid"

        // When & Then - 만료된 토큰이 있어도 검수 시작 가능 (public API)
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/reviews", post.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $expiredToken")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("IN_REVIEW"))
            .andDo(
                documentWithResource(
                    "게시글 검수 시작 - 만료된 토큰",
                    builder()
                        .tag("Post Review")
                        .summary("게시글 검수 시작 - 토큰 무시")
                        .description(
                            """
                            검수 시작 API는 공개 API이므로 만료되거나 유효하지 않은 토큰이 있어도
                            검수를 시작할 수 있습니다. 이 경우 비로그인 사용자로 처리됩니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {만료된 JWT 토큰}").optional(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("검수를 시작할 게시글 ID"),
                        )
                        .responseHeaders(
                            headerWithName(HttpHeaders.LOCATION)
                                .description("생성된 검수의 URI"),
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
