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
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.PostTag
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.port.PostIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import me.helloc.techwikiplus.post.domain.service.port.TagIdGenerator
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
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Instant

/**
 * ReadPostController E2E 테스트
 *
 * - 전체 애플리케이션 컨텍스트 로드
 * - TestContainers를 통한 실제 DB 연동
 * - 운영 환경과 동일한 설정
 * - End-to-End 검증
 * - API 문서 자동 생성 (generateDocs = true)
 * - 변경된 정책: DELETED 상태의 게시글도 정상 조회 가능
 */
@E2eTest(generateDocs = true)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-post",
        "spring.application.version=1.0.0-INTEGRATION",
        "api.documentation.enabled=true",
    ],
)
class ReadPostControllerE2eTest : BaseE2eTest() {
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
    fun `GET posts-postId - 존재하는 게시글을 조회하면 200 OK를 반환해야 한다`() {
        // Given - 테스트 게시글 생성
        val post =
            createTestPost(
                title = "Spring Boot 3.0 새로운 기능들",
                body = "Spring Boot 3.0에서 추가된 새로운 기능들을 소개합니다. GraalVM 네이티브 이미지 지원이 개선되었습니다.",
                status = PostStatus.REVIEWED,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(post.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(post.title.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.body").value(post.body.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(post.status.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.tags").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.createdAt").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.modifiedAt").exists())
            .andDo(
                documentWithResource(
                    "게시글 조회 성공",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 조회")
                        .description(
                            """
                            게시글 ID로 특정 게시글을 조회합니다.
                            
                            게시글이 성공적으로 조회되면 200 OK 상태 코드와 함께
                            게시글의 상세 정보가 반환됩니다.
                            모든 상태(DRAFT, IN_REVIEW, REVIEWED, DELETED)의 게시글을 조회할 수 있습니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("조회할 게시글의 ID"),
                        )
                        .responseFields(
                            fieldWithPath("id")
                                .type(JsonFieldType.STRING)
                                .description("게시글 ID"),
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("게시글 본문"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("게시글 상태 (DRAFT, IN_REVIEW, REVIEWED, DELETED)"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 태그 목록"),
                            fieldWithPath("createdAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 생성 시간"),
                            fieldWithPath("modifiedAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 수정 시간"),
                        )
                        .responseSchema(
                            schema(
                                "${ReadPostController::class.simpleName}" +
                                    ".${ReadPostController.Response::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts-postId - 태그가 포함된 게시글을 조회하면 태그 정보와 함께 반환해야 한다`() {
        // Given - 태그를 먼저 DB에 생성
        val now = Instant.now()
        val kotlinTag =
            tagJpaRepository.save(
                TagEntity(
                    id = tagIdGenerator.next().value,
                    name = "kotlin",
                    postCount = 0,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        val springTag =
            tagJpaRepository.save(
                TagEntity(
                    id = tagIdGenerator.next().value,
                    name = "spring",
                    postCount = 0,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        val jpaTag =
            tagJpaRepository.save(
                TagEntity(
                    id = tagIdGenerator.next().value,
                    name = "jpa",
                    postCount = 0,
                    createdAt = now,
                    updatedAt = now,
                ),
            )

        // 태그가 포함된 게시글 생성
        val tags =
            listOf(
                PostTag(TagName("kotlin"), 1),
                PostTag(TagName("spring"), 2),
                PostTag(TagName("jpa"), 3),
            )
        val post =
            createTestPost(
                title = "Kotlin과 Spring Boot 통합 가이드",
                body = "Kotlin을 사용하여 Spring Boot 애플리케이션을 개발하는 방법을 상세히 설명합니다. JPA 활용법도 포함되어 있습니다.",
                status = PostStatus.REVIEWED,
                tags = tags,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(post.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.tags").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.tags.length()").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$.tags[0].name").value("kotlin"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.tags[0].displayOrder").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.tags[1].name").value("spring"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.tags[1].displayOrder").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.tags[2].name").value("jpa"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.tags[2].displayOrder").value(3))
            .andDo(
                documentWithResource(
                    "태그가 포함된 게시글 조회",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 조회 - 태그 포함")
                        .description(
                            """
                            태그가 포함된 게시글을 조회합니다.
                            
                            태그 정보는 displayOrder 순서대로 정렬되어 반환됩니다.
                            각 태그는 name과 displayOrder 속성을 가집니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("조회할 게시글의 ID"),
                        )
                        .responseFields(
                            fieldWithPath("id")
                                .type(JsonFieldType.STRING)
                                .description("게시글 ID"),
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("게시글 본문"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("게시글 상태"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 태그 목록"),
                            fieldWithPath("tags[].name")
                                .type(JsonFieldType.STRING)
                                .description("태그 이름"),
                            fieldWithPath("tags[].displayOrder")
                                .type(JsonFieldType.NUMBER)
                                .description("태그 표시 순서"),
                            fieldWithPath("createdAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 생성 시간"),
                            fieldWithPath("modifiedAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 수정 시간"),
                        )
                        .responseSchema(
                            schema(
                                "${ReadPostController::class.simpleName}" +
                                    ".${ReadPostController.Response::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts-postId - DELETED 상태의 게시글도 정상적으로 조회되어야 한다`() {
        // Given - DELETED 상태의 게시글 생성
        val post =
            createTestPost(
                title = "삭제된 게시글",
                body = "이 게시글은 삭제된 상태이지만, 정책 변경으로 인해 조회가 가능해야 합니다.",
                status = PostStatus.DELETED,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(post.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(post.title.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.body").value(post.body.value))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("DELETED"))
            .andDo(
                documentWithResource(
                    "삭제된 게시글 조회",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 조회 - DELETED 상태")
                        .description(
                            """
                            삭제된(DELETED) 상태의 게시글을 조회합니다.
                            
                            소프트 삭제 정책에 따라 DELETED 상태의 게시글도
                            조회가 가능합니다. 이를 통해 삭제된 컨텐츠의
                            복구나 히스토리 추적이 가능합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("조회할 게시글의 ID"),
                        )
                        .responseFields(
                            fieldWithPath("id")
                                .type(JsonFieldType.STRING)
                                .description("게시글 ID"),
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("게시글 본문"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("게시글 상태 (DELETED)"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 태그 목록"),
                            fieldWithPath("createdAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 생성 시간"),
                            fieldWithPath("modifiedAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 수정 시간"),
                        )
                        .responseSchema(
                            schema(
                                "${ReadPostController::class.simpleName}" +
                                    ".${ReadPostController.Response::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts-postId - DRAFT 상태의 게시글을 조회할 수 있어야 한다`() {
        // Given - DRAFT 상태의 게시글 생성
        val post =
            createTestPost(
                title = "작성 중인 게시글",
                body = "아직 작성 중인 게시글입니다. DRAFT 상태이지만 조회가 가능해야 합니다.",
                status = PostStatus.DRAFT,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(post.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("DRAFT"))
    }

    @Test
    fun `GET posts-postId - IN_REVIEW 상태의 게시글을 조회할 수 있어야 한다`() {
        // Given - IN_REVIEW 상태의 게시글 생성
        val post =
            createTestPost(
                title = "검토 중인 게시글",
                body = "현재 검토 중인 게시글입니다. IN_REVIEW 상태에서도 조회가 가능해야 합니다.",
                status = PostStatus.IN_REVIEW,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(post.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("IN_REVIEW"))
    }

    @Test
    fun `GET posts-postId - 존재하지 않는 게시글을 조회하면 404 Not Found를 반환해야 한다`() {
        // Given - 존재하지 않는 게시글 ID
        val nonExistentId = 9999999L

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}", nonExistentId)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("POST_NOT_FOUND"))
            .andDo(
                documentWithResource(
                    "존재하지 않는 게시글 조회",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 조회 - 존재하지 않는 게시글")
                        .description(
                            """
                            존재하지 않는 게시글 ID로 조회를 시도하면 404 Not Found를 반환합니다.
                            
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
    fun `GET posts-postId - 잘못된 형식의 ID로 조회하면 400 Bad Request를 반환해야 한다`() {
        // Given - 잘못된 형식의 ID
        val invalidId = "invalid-id"

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}", invalidId)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andDo(
                documentWithResource(
                    "잘못된 형식의 ID로 게시글 조회",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 조회 - 잘못된 ID 형식")
                        .description(
                            """
                            숫자가 아닌 잘못된 형식의 ID로 조회를 시도하면 400 Bad Request를 반환합니다.
                            
                            게시글 ID는 숫자(Long) 형식이어야 합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("조회할 게시글의 ID (숫자여야 함)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts-postId - 음수 ID로 조회하면 400 Bad Request를 반환해야 한다`() {
        // Given - 음수 ID
        val negativeId = -1L

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}", negativeId)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_POST_ID_FORMAT"))
            .andDo(
                documentWithResource(
                    "음수 ID로 게시글 조회",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 조회 - 음수 ID")
                        .description(
                            """
                            음수 ID로 조회를 시도하면 400 Bad Request를 반환합니다.
                            
                            게시글 ID는 양수여야 합니다.
                            에러 코드: INVALID_POST_ID_FORMAT
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("조회할 게시글의 ID (양수여야 함)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts-postId - 인증 없이도 게시글을 조회할 수 있어야 한다`() {
        // Given - 게시글 생성 (인증 토큰 없이 조회)
        val post =
            createTestPost(
                title = "공개 게시글",
                body = "인증 없이도 조회 가능한 공개 게시글입니다. 누구나 읽을 수 있습니다.",
                status = PostStatus.REVIEWED,
            )

        // When & Then - 인증 헤더 없이 요청
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(post.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(post.title.value))
            .andDo(
                documentWithResource(
                    "인증 없이 게시글 조회",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 조회 - 인증 불필요")
                        .description(
                            """
                            게시글 조회는 인증이 필요하지 않습니다.
                            
                            누구나 게시글을 조회할 수 있으며,
                            Authorization 헤더 없이도 요청이 가능합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("조회할 게시글의 ID"),
                        )
                        .responseFields(
                            fieldWithPath("id")
                                .type(JsonFieldType.STRING)
                                .description("게시글 ID"),
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("게시글 본문"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("게시글 상태"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 태그 목록"),
                            fieldWithPath("createdAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 생성 시간"),
                            fieldWithPath("modifiedAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 수정 시간"),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `GET posts-postId - 인증된 사용자도 게시글을 조회할 수 있어야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser()
        val token = jwtTokenManager.generateAccessToken(user.id).token

        val post =
            createTestPost(
                title = "인증된 사용자가 조회하는 게시글",
                body = "인증된 사용자도 당연히 게시글을 조회할 수 있습니다. 인증 토큰이 있어도 게시글 조회는 정상적으로 동작합니다.",
                status = PostStatus.REVIEWED,
            )

        // When & Then - 인증 헤더와 함께 요청
        mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}", post.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(post.id.value.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(post.title.value))
            .andDo(
                documentWithResource(
                    "인증된 사용자의 게시글 조회",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 조회 - 인증된 사용자")
                        .description(
                            """
                            인증된 사용자도 게시글을 조회할 수 있습니다.
                            
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
                            parameterWithName("postId")
                                .description("조회할 게시글의 ID"),
                        )
                        .responseFields(
                            fieldWithPath("id")
                                .type(JsonFieldType.STRING)
                                .description("게시글 ID"),
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("게시글 본문"),
                            fieldWithPath("status")
                                .type(JsonFieldType.STRING)
                                .description("게시글 상태"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 태그 목록"),
                            fieldWithPath("createdAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 생성 시간"),
                            fieldWithPath("modifiedAt")
                                .type(JsonFieldType.STRING)
                                .description("게시글 수정 시간"),
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
        body: String = "테스트 게시글 본문입니다. 충분한 길이의 컨텐츠를 포함하고 있습니다.",
        status: PostStatus = PostStatus.REVIEWED,
        tags: List<PostTag> = emptyList(),
    ): Post {
        val now = Instant.now()
        val postId = postIdGenerator.next()

        val post =
            Post(
                id = postId,
                title = PostTitle(title),
                body = PostBody(body),
                status = status,
                tags = tags,
                createdAt = now,
                updatedAt = now,
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
}
