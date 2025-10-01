package me.helloc.techwikiplus.post.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters.Companion.builder
import com.epages.restdocs.apispec.Schema.Companion.schema
import me.helloc.techwikiplus.common.config.BaseE2eTest
import me.helloc.techwikiplus.common.config.annotations.E2eTest
import me.helloc.techwikiplus.common.config.documentation.withStandardErrorResponse
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.TagJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.TagEntity
import me.helloc.techwikiplus.common.infrastructure.security.jwt.JwtTokenManager
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostRevisionVersion
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.PostTag
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.port.PostIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import me.helloc.techwikiplus.post.domain.service.port.TagIdGenerator
import me.helloc.techwikiplus.post.dto.response.PostScrollResponse
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
 * ReadPostScrollController E2E 테스트
 *
 * - 전체 애플리케이션 컨텍스트 로드
 * - TestContainers를 통한 실제 DB 연동
 * - 운영 환경과 동일한 설정
 * - End-to-End 검증
 * - API 문서 자동 생성 (generateDocs = true)
 * - 커서 기반 페이징 테스트
 */
@E2eTest(generateDocs = true)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-post",
        "spring.application.version=1.0.0-INTEGRATION",
        "api.documentation.enabled=true",
    ],
)
class PostScrollQueryControllerE2eTest : BaseE2eTest() {
    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var postIdGenerator: PostIdGenerator

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenManager: JwtTokenManager

    @Autowired
    private lateinit var tagJpaRepository: TagJpaRepository

    @Autowired
    private lateinit var tagIdGenerator: TagIdGenerator

    @Test
    fun `GET posts - 커서 없이 요청하면 첫 페이지를 반환해야 한다`() {
        // Given - 여러 개의 게시글 생성
        (1..25).map { i ->
            createTestPost(
                title = "테스트 게시글 $i",
                body = "이것은 테스트 게시글 $i 번의 본문입니다. 충분한 길이의 컨텐츠를 포함하고 있습니다.",
                status = if (i % 2 == 0) PostStatus.REVIEWED else PostStatus.DRAFT,
            )
        }

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .param("limit", "10")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts.length()").value(10))
            .andExpect(MockMvcResultMatchers.jsonPath("$.hasNext").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.nextCursor").exists())
            .andDo(
                documentWithResource(
                    "게시글 목록 조회 - 첫 페이지",
                    builder()
                        .tag("Post")
                        .summary("게시글 목록 조회")
                        .description(
                            """
                            커서 기반 페이징으로 게시글 목록을 조회합니다.
                            
                            커서를 제공하지 않으면 첫 페이지를 반환합니다.
                            반환된 nextCursor를 사용하여 다음 페이지를 조회할 수 있습니다.
                            최신 게시글부터 오래된 순서로 정렬되어 반환됩니다.
                            """.trimIndent(),
                        )
                        .queryParameters(
                            parameterWithName("cursor")
                                .description("이전 응답에서 받은 커서 값 (첫 페이지는 생략)")
                                .optional(),
                            parameterWithName("limit")
                                .description("한 페이지에 반환할 게시글 수 (기본값: 20, 최대: 100)")
                                .optional(),
                        )
                        .responseFields(
                            fieldWithPath("posts")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 목록"),
                            fieldWithPath("posts[].id")
                                .type(JsonFieldType.STRING)
                                .description("게시글 ID"),
                            fieldWithPath("posts[].title")
                                .type(JsonFieldType.STRING)
                                .description("게시글 제목"),
                            fieldWithPath("posts[].summary")
                                .type(JsonFieldType.STRING)
                                .description("게시글 요약"),
                            fieldWithPath("posts[].status")
                                .type(JsonFieldType.STRING)
                                .description("게시글 상태 (DRAFT, IN_REVIEW, REVIEWED, DELETED)"),
                            fieldWithPath("posts[].tags")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 태그 목록"),
                            fieldWithPath("posts[].createdAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 생성 시간"),
                            fieldWithPath("posts[].updatedAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 수정 시간"),
                            fieldWithPath("hasNext")
                                .type(JsonFieldType.BOOLEAN)
                                .description("다음 페이지 존재 여부"),
                            fieldWithPath("nextCursor")
                                .type(JsonFieldType.STRING)
                                .description("다음 페이지 조회를 위한 커서 (hasNext가 false일 때는 null)")
                                .optional(),
                        )
                        .responseSchema(
                            schema(
                                "${PostScrollResponse::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts - 커서를 사용하여 다음 페이지를 조회할 수 있어야 한다`() {
        // Given - 여러 개의 게시글 생성
        (1..30).map { i ->
            Thread.sleep(10) // ID 생성 시간 차이를 위해
            createTestPost(
                title = "페이징 테스트 게시글 $i",
                body = "이것은 페이징 테스트를 위한 게시글 $i 번의 본문입니다. 커서 기반 페이징을 검증합니다.",
                status = PostStatus.REVIEWED,
            )
        }.sortedByDescending { it.id.value }

        // 첫 페이지 조회
        val firstPageResponse =
            mockMvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/posts")
                    .param("limit", "10")
                    .accept(MediaType.APPLICATION_JSON),
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
                .response
                .contentAsString

        val firstPageJson = objectMapper.readTree(firstPageResponse)
        val nextCursor = firstPageJson.get("nextCursor").asText()

        // When & Then - 두 번째 페이지 조회
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .param("cursor", nextCursor)
                .param("limit", "10")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts.length()").value(10))
            .andExpect(MockMvcResultMatchers.jsonPath("$.hasNext").value(true))
            .andDo(
                documentWithResource(
                    "게시글 목록 조회 - 다음 페이지",
                    builder()
                        .tag("Post")
                        .summary("게시글 목록 조회 - 커서 사용")
                        .description(
                            """
                            커서를 사용하여 다음 페이지의 게시글 목록을 조회합니다.
                            
                            이전 응답에서 받은 nextCursor 값을 cursor 파라미터로 전달하면
                            해당 위치부터 다음 게시글들을 조회할 수 있습니다.
                            """.trimIndent(),
                        )
                        .queryParameters(
                            parameterWithName("cursor")
                                .description("이전 응답에서 받은 nextCursor 값"),
                            parameterWithName("limit")
                                .description("한 페이지에 반환할 게시글 수")
                                .optional(),
                        )
                        .responseSchema(
                            schema(
                                "${PostScrollResponse::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts - 마지막 페이지에서는 hasNext가 false를 반환해야 한다`() {
        // Given - 적은 수의 게시글 생성
        (1..5).map { i ->
            createTestPost(
                title = "소량 게시글 $i",
                body = "마지막 페이지 테스트를 위한 게시글 $i 번입니다. 총 5개만 생성합니다.",
                status = PostStatus.REVIEWED,
            )
        }

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .param("limit", "10")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts.length()").value(5))
            .andExpect(MockMvcResultMatchers.jsonPath("$.hasNext").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.nextCursor").doesNotExist())
            .andDo(
                documentWithResource(
                    "게시글 목록 조회 - 마지막 페이지",
                    builder()
                        .tag("Post")
                        .summary("게시글 목록 조회 - 마지막 페이지")
                        .description(
                            """
                            더 이상 조회할 게시글이 없는 마지막 페이지입니다.
                            
                            hasNext가 false이고 nextCursor가 null일 때,
                            현재 페이지가 마지막 페이지임을 의미합니다.
                            """.trimIndent(),
                        )
                        .queryParameters(
                            parameterWithName("limit")
                                .description("한 페이지에 반환할 게시글 수")
                                .optional(),
                        )
                        .responseFields(
                            fieldWithPath("posts")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 목록"),
                            fieldWithPath("posts[].id")
                                .type(JsonFieldType.STRING)
                                .description("게시글 ID"),
                            fieldWithPath("posts[].title")
                                .type(JsonFieldType.STRING)
                                .description("게시글 제목"),
                            fieldWithPath("posts[].summary")
                                .type(JsonFieldType.STRING)
                                .description("게시글 요약"),
                            fieldWithPath("posts[].status")
                                .type(JsonFieldType.STRING)
                                .description("게시글 상태"),
                            fieldWithPath("posts[].tags")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 태그 목록"),
                            fieldWithPath("posts[].createdAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 생성 시간"),
                            fieldWithPath("posts[].updatedAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 수정 시간"),
                            fieldWithPath("hasNext")
                                .type(JsonFieldType.BOOLEAN)
                                .description("다음 페이지 존재 여부 (false)"),
                            fieldWithPath("nextCursor")
                                .type(JsonFieldType.STRING)
                                .description("다음 페이지 커서 (null)")
                                .optional(),
                        )
                        .responseSchema(
                            schema(
                                "${PostScrollResponse::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts - 태그가 포함된 게시글들도 정상적으로 조회되어야 한다`() {
        // Given - 태그 생성
        createTestTag("kotlin", 10)
        createTestTag("spring", 8)
        createTestTag("java", 5)

        // 태그가 있는 게시글들 생성
        (1..5).map { i ->
            val tags =
                when (i) {
                    1 ->
                        setOf(
                            PostTag(TagName("kotlin"), 0),
                            PostTag(TagName("spring"), 1),
                        )
                    2 ->
                        setOf(
                            PostTag(TagName("java"), 0),
                        )
                    3 ->
                        setOf(
                            PostTag(TagName("kotlin"), 0),
                            PostTag(TagName("java"), 1),
                            PostTag(TagName("spring"), 2),
                        )
                    else -> emptySet()
                }

            createTestPost(
                title = "태그 포함 게시글 $i",
                body = "태그가 포함된 게시글 $i 번입니다. 태그 정보가 함께 반환되어야 합니다.",
                status = PostStatus.REVIEWED,
                tags = tags,
            )
        }

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .param("limit", "5")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts.length()").value(5))
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts[0].tags").isArray)
            .andDo(
                documentWithResource(
                    "게시글 목록 조회 - 태그 포함",
                    builder()
                        .tag("Post")
                        .summary("게시글 목록 조회 - 태그 정보 포함")
                        .description(
                            """
                            태그가 포함된 게시글 목록을 조회합니다.
                            
                            각 게시글의 태그 정보가 함께 반환되며,
                            태그는 displayOrder 순서대로 정렬되어 있습니다.
                            """.trimIndent(),
                        )
                        .queryParameters(
                            parameterWithName("limit")
                                .description("한 페이지에 반환할 게시글 수")
                                .optional(),
                        )
                        .responseFields(
                            fieldWithPath("posts")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 목록"),
                            fieldWithPath("posts[].id")
                                .type(JsonFieldType.STRING)
                                .description("게시글 ID"),
                            fieldWithPath("posts[].title")
                                .type(JsonFieldType.STRING)
                                .description("게시글 제목"),
                            fieldWithPath("posts[].summary")
                                .type(JsonFieldType.STRING)
                                .description("게시글 요약"),
                            fieldWithPath("posts[].status")
                                .type(JsonFieldType.STRING)
                                .description("게시글 상태"),
                            fieldWithPath("posts[].tags")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 태그 목록"),
                            fieldWithPath("posts[].tags[].name")
                                .type(JsonFieldType.STRING)
                                .description("태그 이름")
                                .optional(),
                            fieldWithPath("posts[].tags[].displayOrder")
                                .type(JsonFieldType.NUMBER)
                                .description("태그 표시 순서")
                                .optional(),
                            fieldWithPath("posts[].createdAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 생성 시간"),
                            fieldWithPath("posts[].updatedAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 수정 시간"),
                            fieldWithPath("hasNext")
                                .type(JsonFieldType.BOOLEAN)
                                .description("다음 페이지 존재 여부"),
                            fieldWithPath("nextCursor")
                                .type(JsonFieldType.STRING)
                                .description("다음 페이지 커서")
                                .optional(),
                        )
                        .responseSchema(
                            schema(
                                "PostScrollResponse",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts - limit 파라미터를 지정하지 않으면 기본값 20개를 반환해야 한다`() {
        // Given - 25개의 게시글 생성
        (1..25).map { i ->
            createTestPost(
                title = "기본 limit 테스트 게시글 $i",
                body = "limit 파라미터 기본값 테스트를 위한 게시글 $i 번입니다.",
                status = PostStatus.REVIEWED,
            )
        }

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts.length()").value(20))
            .andExpect(MockMvcResultMatchers.jsonPath("$.hasNext").value(true))
    }

    @Test
    fun `GET posts - limit이 100을 초과하면 400 Bad Request를 반환해야 한다`() {
        // Given - 게시글 생성
        createTestPost(
            title = "Limit 검증 테스트 게시글",
            body = "limit 파라미터 검증을 위한 게시글입니다. 최소 30자 이상의 내용이 필요합니다.",
            status = PostStatus.REVIEWED,
        )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .param("limit", "101")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_PAGINATION_LIMIT"))
            .andDo(
                documentWithResource(
                    "게시글 목록 조회 - limit 초과",
                    builder()
                        .tag("Post")
                        .summary("게시글 목록 조회 - 잘못된 limit")
                        .description(
                            """
                            limit 값이 최대값(100)을 초과하면 400 Bad Request를 반환합니다.
                            
                            limit은 1 이상 100 이하의 값이어야 합니다.
                            """.trimIndent(),
                        )
                        .queryParameters(
                            parameterWithName("limit")
                                .description("한 페이지에 반환할 게시글 수 (최대 100)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts - limit이 0 이하면 400 Bad Request를 반환해야 한다`() {
        // Given - 게시글 생성
        createTestPost(
            title = "Limit 검증 테스트 게시글",
            body = "limit 파라미터 검증을 위한 게시글입니다. 최소 30자 이상의 내용이 필요합니다.",
            status = PostStatus.REVIEWED,
        )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .param("limit", "0")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_PAGINATION_LIMIT"))
            .andDo(
                documentWithResource(
                    "게시글 목록 조회 - limit 0",
                    builder()
                        .tag("Post")
                        .summary("게시글 목록 조회 - 0 이하 limit")
                        .description(
                            """
                            limit 값이 0 이하면 400 Bad Request를 반환합니다.
                            
                            limit은 1 이상의 값이어야 합니다.
                            """.trimIndent(),
                        )
                        .queryParameters(
                            parameterWithName("limit")
                                .description("한 페이지에 반환할 게시글 수 (1 이상)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts - 잘못된 형식의 커서를 전달하면 400 Bad Request를 반환해야 한다`() {
        // Given - 게시글 생성
        createTestPost(
            title = "커서 검증 테스트 게시글",
            body = "잘못된 커서 파라미터 검증을 위한 게시글입니다. 최소 30자 이상의 내용이 필요합니다.",
            status = PostStatus.REVIEWED,
        )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .param("cursor", "invalid-cursor")
                .param("limit", "10")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andDo(
                documentWithResource(
                    "게시글 목록 조회 - 잘못된 커서",
                    builder()
                        .tag("Post")
                        .summary("게시글 목록 조회 - 잘못된 커서 형식")
                        .description(
                            """
                            잘못된 형식의 커서를 전달하면 400 Bad Request를 반환합니다.
                            
                            커서는 숫자 형식의 게시글 ID여야 합니다.
                            """.trimIndent(),
                        )
                        .queryParameters(
                            parameterWithName("cursor")
                                .description("잘못된 형식의 커서"),
                            parameterWithName("limit")
                                .description("한 페이지에 반환할 게시글 수")
                                .optional(),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts - 음수 커서를 전달하면 400 Bad Request를 반환해야 한다`() {
        // Given - 게시글 생성
        createTestPost(
            title = "음수 커서 테스트 게시글",
            body = "음수 커서 파라미터 검증을 위한 게시글입니다. 최소 30자 이상의 내용이 필요합니다.",
            status = PostStatus.REVIEWED,
        )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .param("cursor", "-1")
                .param("limit", "10")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_POST_ID_FORMAT"))
            .andDo(
                documentWithResource(
                    "게시글 목록 조회 - 음수 커서",
                    builder()
                        .tag("Post")
                        .summary("게시글 목록 조회 - 음수 커서")
                        .description(
                            """
                            음수 커서를 전달하면 400 Bad Request를 반환합니다.
                            
                            커서는 양수여야 합니다.
                            """.trimIndent(),
                        )
                        .queryParameters(
                            parameterWithName("cursor")
                                .description("음수 커서 값"),
                            parameterWithName("limit")
                                .description("한 페이지에 반환할 게시글 수")
                                .optional(),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts - 게시글이 없을 때 빈 배열을 반환해야 한다`() {
        // Given - 게시글이 없는 상태

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .param("limit", "10")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts.length()").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.hasNext").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.nextCursor").doesNotExist())
            .andDo(
                documentWithResource(
                    "게시글 목록 조회 - 빈 목록",
                    builder()
                        .tag("Post")
                        .summary("게시글 목록 조회 - 게시글 없음")
                        .description(
                            """
                            게시글이 없을 때 빈 배열을 반환합니다.
                            
                            posts는 빈 배열이고, hasNext는 false입니다.
                            """.trimIndent(),
                        )
                        .queryParameters(
                            parameterWithName("limit")
                                .description("한 페이지에 반환할 게시글 수")
                                .optional(),
                        )
                        .responseFields(
                            fieldWithPath("posts")
                                .type(JsonFieldType.ARRAY)
                                .description("빈 게시글 목록"),
                            fieldWithPath("hasNext")
                                .type(JsonFieldType.BOOLEAN)
                                .description("다음 페이지 존재 여부 (false)"),
                            fieldWithPath("nextCursor")
                                .type(JsonFieldType.STRING)
                                .description("다음 페이지 커서 (null)")
                                .optional(),
                        )
                        .responseSchema(
                            schema(
                                "${PostScrollResponse::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts - 인증 없이도 게시글 목록을 조회할 수 있어야 한다`() {
        // Given - 게시글 생성
        (1..5).map { i ->
            createTestPost(
                title = "공개 게시글 $i",
                body = "인증 없이도 조회 가능한 공개 게시글 $i 번입니다. 공개된 컨텐츠로 모두 열람 가능합니다.",
                status = PostStatus.REVIEWED,
            )
        }

        // When & Then - 인증 헤더 없이 요청
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .param("limit", "5")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts.length()").value(5))
            .andDo(
                documentWithResource(
                    "게시글 목록 조회 - 인증 불필요",
                    builder()
                        .tag("Post")
                        .summary("게시글 목록 조회 - 인증 없이")
                        .description(
                            """
                            게시글 목록 조회는 인증이 필요하지 않습니다.
                            
                            누구나 게시글 목록을 조회할 수 있으며,
                            Authorization 헤더 없이도 요청이 가능합니다.
                            """.trimIndent(),
                        )
                        .queryParameters(
                            parameterWithName("limit")
                                .description("한 페이지에 반환할 게시글 수")
                                .optional(),
                        )
                        .responseSchema(
                            schema(
                                "${PostScrollResponse::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts - 인증된 사용자도 게시글 목록을 조회할 수 있어야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser()
        val token = jwtTokenManager.generateAccessToken(user.id).token

        // 게시글 생성
        (1..3).map { i ->
            createTestPost(
                title = "인증 사용자 조회 게시글 $i",
                body = "인증된 사용자가 조회하는 게시글 $i 번입니다. 인증 토큰을 통해 보호된 리소스를 접근합니다.",
                status = PostStatus.REVIEWED,
            )
        }

        // When & Then - 인증 헤더와 함께 요청
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .param("limit", "5")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts.length()").value(3))
            .andDo(
                documentWithResource(
                    "게시글 목록 조회 - 인증된 사용자",
                    builder()
                        .tag("Post")
                        .summary("게시글 목록 조회 - 인증 사용자")
                        .description(
                            """
                            인증된 사용자도 게시글 목록을 조회할 수 있습니다.
                            
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
                        .queryParameters(
                            parameterWithName("limit")
                                .description("한 페이지에 반환할 게시글 수")
                                .optional(),
                        )
                        .responseSchema(
                            schema(
                                "${PostScrollResponse::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts - DELETED 상태의 게시글은 목록에서 제외되어야 한다`() {
        // Given - 다양한 상태의 게시글 생성
        createTestPost(
            title = "초안 게시글",
            body = "DRAFT 상태의 게시글입니다. 아직 공개되지 않은 초안 상태의 컨텐츠를 포함하고 있습니다.",
            status = PostStatus.DRAFT,
        )
        createTestPost(
            title = "검토 중 게시글",
            body = "IN_REVIEW 상태의 게시글입니다. 현재 검토 중이며 곧 공개될 예정의 컨텐츠입니다.",
            status = PostStatus.IN_REVIEW,
        )
        createTestPost(
            title = "검토 완료 게시글",
            body = "REVIEWED 상태의 게시글입니다. 검토가 완료되어 공개된 컨텐츠로 일반 사용자들이 열람 가능합니다.",
            status = PostStatus.REVIEWED,
        )
        createTestPost(
            title = "삭제된 게시글",
            body = "DELETED 상태의 게시글입니다. 소프트 삭제로 더 이상 표시되지 않지만 데이터는 보존됩니다.",
            status = PostStatus.DELETED,
        )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts")
                .param("limit", "10")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts.length()").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$.posts[?(@.status == 'DELETED')]").doesNotExist())
            .andDo(
                documentWithResource(
                    "게시글 목록 조회 - 삭제된 게시글 제외",
                    builder()
                        .tag("Post")
                        .summary("게시글 목록 조회 - 삭제된 게시글 제외")
                        .description(
                            """
                            DELETED 상태를 제외한 게시글 목록을 조회합니다.
                            
                            DRAFT, IN_REVIEW, REVIEWED 상태의 게시글만
                            조회 결과에 포함됩니다.
                            소프트 삭제 정책에 따라 DELETED 상태의 게시글은
                            목록에서 제외되어 표시되지 않습니다.
                            """.trimIndent(),
                        )
                        .queryParameters(
                            parameterWithName("limit")
                                .description("한 페이지에 반환할 게시글 수")
                                .optional(),
                        )
                        .responseSchema(
                            schema(
                                "${PostScrollResponse::class.simpleName}",
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
        title: String = "테스트 게시글",
        body: String = "테스트 게시글 본문입니다. 충분한 길이의 컨텐츠를 포함하고 있습니다. 최소 30자 이상의 내용이 필요합니다.",
        status: PostStatus = PostStatus.REVIEWED,
        tags: Set<PostTag> = emptySet(),
    ): Post {
        val now = Instant.now()
        val postId = postIdGenerator.next()

        val post =
            Post.create(
                id = postId,
                title = PostTitle(title),
                body = PostBody(body),
                status = status,
                version = PostRevisionVersion(),
                postTags = tags,
                createdAt = now,
                updatedAt = now,
            )

        return postRepository.save(post)
    }

    /**
     * 테스트용 태그 생성 헬퍼 메서드
     */
    private fun createTestTag(
        name: String,
        postCount: Int,
    ): TagEntity {
        val now = Instant.now()
        val tag =
            TagEntity(
                id = tagIdGenerator.next().value,
                name = name,
                postCount = postCount,
                createdAt = now,
                updatedAt = now,
            )
        return tagJpaRepository.save(tag)
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
}
