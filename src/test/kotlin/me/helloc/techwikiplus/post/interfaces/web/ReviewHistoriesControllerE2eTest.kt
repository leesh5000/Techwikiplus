package me.helloc.techwikiplus.post.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters.Companion.builder
import com.epages.restdocs.apispec.Schema
import me.helloc.techwikiplus.common.config.BaseE2eTest
import me.helloc.techwikiplus.common.config.annotations.E2eTest
import me.helloc.techwikiplus.common.config.documentation.withStandardErrorResponse
import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
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
 * ReviewHistoriesController E2E 테스트
 *
 * - 게시글의 모든 리뷰 내역을 조회하는 API 테스트
 * - TestContainers를 통한 실제 DB 연동
 * - API 문서 자동 생성 (generateDocs = true)
 */
@E2eTest(generateDocs = true)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-review-histories",
        "spring.application.version=1.0.0-INTEGRATION",
        "api.documentation.enabled=true",
    ],
)
class ReviewHistoriesControllerE2eTest : BaseE2eTest() {
    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var postReviewRepository: PostReviewRepository

    @Autowired
    private lateinit var postIdGenerator: PostIdGenerator

    @Autowired
    private lateinit var postReviewIdGenerator: PostReviewIdGenerator

    @Autowired
    private lateinit var clockHolder: ClockHolder

    @Test
    fun `GET review-histories - 리뷰 내역이 있는 게시글을 조회하면 200 OK와 리뷰 목록을 반환해야 한다`() {
        // Given - 게시글과 여러 리뷰 생성
        val post = createTestPost()

        // 완료된 리뷰
        val completedReview =
            createTestReview(
                post = post,
                status = PostReviewStatus.COMPLETED,
                winningRevisionId = PostRevisionId(12345L),
                // 2시간 전 시작
                startedAtOffset = -7200,
            )

        // 진행 중인 리뷰
        val inProgressReview =
            createTestReview(
                post = post,
                status = PostReviewStatus.IN_REVIEW,
                // 1시간 전 시작
                startedAtOffset = -3600,
            )

        // 취소된 리뷰
        val cancelledReview =
            createTestReview(
                post = post,
                status = PostReviewStatus.CANCELLED,
                // 3시간 전 시작
                startedAtOffset = -10800,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/reviews/histories", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].reviewId").value(inProgressReview.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("IN_REVIEW"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].reviewId").value(completedReview.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].status").value("COMPLETED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].winningRevisionId").value(12345L))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].reviewId").value(cancelledReview.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].status").value("CANCELLED"))
            .andDo(
                documentWithResource(
                    "리뷰 내역 조회 성공",
                    builder()
                        .tag("Review History")
                        .summary("게시글 리뷰 내역 조회")
                        .description(
                            """
                            특정 게시글의 모든 리뷰 내역을 조회합니다.

                            리뷰 내역은 최신순(startedAt 기준)으로 정렬되어 반환됩니다.
                            각 리뷰의 상태(IN_REVIEW, COMPLETED, CANCELLED)와
                            완료된 경우 선정된 수정본 ID가 포함됩니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("조회할 게시글의 ID"),
                        )
                        .responseFields(
                            fieldWithPath("[].reviewId")
                                .type(JsonFieldType.NUMBER)
                                .description("리뷰 ID"),
                            fieldWithPath("[].startedAt")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 시작 시간 (Instant)"),
                            fieldWithPath("[].deadline")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 마감 시간 (ISO-8601 형식)"),
                            fieldWithPath("[].status")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 상태 (IN_REVIEW, COMPLETED, CANCELLED)"),
                            fieldWithPath("[].winningRevisionId")
                                .type(JsonFieldType.NUMBER)
                                .description("선정된 수정본 ID (COMPLETED 상태일 때만)")
                                .optional(),
                            fieldWithPath("[].completedAt")
                                .type(JsonFieldType.STRING)
                                .description("리뷰 완료 시간 (ISO-8601 형식, COMPLETED 상태일 때만)")
                                .optional(),
                        )
                        .responseSchema(Schema("ReviewHistoryResponseList"))
                        .build(),
                ),
            )
    }

    @Test
    fun `GET review-histories - 리뷰 내역이 없는 게시글을 조회하면 빈 배열을 반환해야 한다`() {
        // Given - 리뷰가 없는 게시글 생성
        val post = createTestPost()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/reviews/histories", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andDo(
                documentWithResource(
                    "리뷰 내역이 없는 경우",
                    builder()
                        .tag("Review History")
                        .summary("리뷰 내역 조회 - 빈 결과")
                        .description(
                            """
                            리뷰 내역이 없는 게시글을 조회하면 빈 배열이 반환됩니다.

                            게시글은 존재하지만 아직 리뷰가 시작되지 않은 경우입니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("조회할 게시글의 ID"),
                        )
                        .responseFields(
                            fieldWithPath("[]")
                                .type(JsonFieldType.ARRAY)
                                .description("빈 리뷰 내역 배열"),
                        )
                        .responseSchema(Schema("ReviewHistoryResponseList"))
                        .build(),
                ),
            )
    }

    @Test
    fun `GET review-histories - 존재하지 않는 게시글을 조회하면 404 Not Found를 반환해야 한다`() {
        // Given - 존재하지 않는 게시글 ID
        val nonExistentId = 9999999L

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/reviews/histories", nonExistentId)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("POST_NOT_FOUND"))
            .andDo(
                documentWithResource(
                    "존재하지 않는 게시글의 리뷰 내역 조회",
                    builder()
                        .tag("Review History")
                        .summary("리뷰 내역 조회 - 게시글 없음")
                        .description(
                            """
                            존재하지 않는 게시글 ID로 리뷰 내역을 조회하면 404 Not Found를 반환합니다.

                            에러 코드: POST_NOT_FOUND
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("조회할 게시글의 ID"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET review-histories - 완료된 리뷰의 completedAt이 정확하게 반환되어야 한다`() {
        // Given - 완료된 리뷰가 있는 게시글
        val post = createTestPost()
        val completedReview =
            createTestReview(
                post = post,
                status = PostReviewStatus.COMPLETED,
                winningRevisionId = PostRevisionId(54321L),
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/reviews/histories", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].reviewId").value(completedReview.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("COMPLETED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].completedAt").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].winningRevisionId").value(54321L))
    }

    @Test
    fun `GET review-histories - 진행 중인 리뷰는 completedAt이 null이어야 한다`() {
        // Given - 진행 중인 리뷰가 있는 게시글
        val post = createTestPost()
        val inProgressReview =
            createTestReview(
                post = post,
                status = PostReviewStatus.IN_REVIEW,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/reviews/histories", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].reviewId").value(inProgressReview.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("IN_REVIEW"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].completedAt").doesNotExist())
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].winningRevisionId").doesNotExist())
    }

    @Test
    fun `GET review-histories - 리뷰 내역이 시간순으로 정렬되어 반환되어야 한다`() {
        // Given - 서로 다른 시간에 시작된 여러 리뷰
        val post = createTestPost()

        val oldReview =
            createTestReview(
                post = post,
                status = PostReviewStatus.CANCELLED,
                // 1일 전
                startedAtOffset = -86400,
            )

        val recentReview =
            createTestReview(
                post = post,
                status = PostReviewStatus.IN_REVIEW,
                // 30분 전
                startedAtOffset = -1800,
            )

        val middleReview =
            createTestReview(
                post = post,
                status = PostReviewStatus.COMPLETED,
                winningRevisionId = PostRevisionId(11111L),
                // 12시간 전
                startedAtOffset = -43200,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/reviews/histories", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
            // 최신순으로 정렬되어야 함
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].reviewId").value(recentReview.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].reviewId").value(middleReview.id.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].reviewId").value(oldReview.id.value))
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
        startedAtOffset: Long = 0,
    ): PostReview {
        val now = clockHolder.now().plusSeconds(startedAtOffset)
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
}
