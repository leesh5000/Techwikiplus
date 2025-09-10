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
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Instant

/**
 * UpdatePostController E2E 테스트
 *
 * - 전체 애플리케이션 컨텍스트 로드
 * - TestContainers를 통한 실제 DB 연동
 * - 운영 환경과 동일한 설정
 * - End-to-End 검증
 * - API 문서 자동 생성 (generateDocs = true)
 * - 게시글 수정 기능 테스트
 */
@E2eTest(generateDocs = true)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-post",
        "spring.application.version=1.0.0-INTEGRATION",
        "api.documentation.enabled=true",
    ],
)
class UpdatePostControllerE2eTest : BaseE2eTest() {
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
    fun `PUT posts - 게시글 제목과 본문을 수정할 수 있어야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser(role = UserRole.ADMIN)
        val accessToken = jwtTokenManager.generateAccessToken(user.id).token

        // Given - 기존 게시글 생성
        val originalPost =
            createTestPost(
                title = "원본 제목",
                body = "원본 본문입니다. 이 내용은 곧 수정될 예정입니다. 최소 30자 이상의 내용이 필요합니다.",
                status = PostStatus.REVIEWED,
            )

        val requestBody =
            """
            {
                "title": "수정된 제목",
                "body": "수정된 본문입니다. 이전 내용과 완전히 다른 새로운 내용으로 변경되었습니다. 충분한 길이의 컨텐츠를 포함하고 있습니다."
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}", originalPost.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
            .andDo(
                documentWithResource(
                    "게시글 수정 - 성공",
                    builder()
                        .tag("Post")
                        .summary("게시글 수정")
                        .description(
                            """
                            게시글의 제목과 본문을 수정합니다.
                            
                            태그 정보를 함께 전달하면 태그도 수정할 수 있습니다.
                            수정 성공 시 204 No Content를 반환합니다.
                            존재하지 않는 게시글 ID를 전달하면 404 Not Found를 반환합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("수정할 게시글 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 본문 (최소 30자)"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 태그 목록")
                                .optional(),
                        )
                        .requestSchema(
                            schema("UpdatePostRequest"),
                        )
                        .build(),
                ),
            )

        // 수정된 내용 확인
        val updatedPost = postRepository.findBy(originalPost.id)
        updatedPost shouldNotBe null
        updatedPost?.title?.value shouldBe "수정된 제목"
        updatedPost?.body?.value shouldBe "수정된 본문입니다. 이전 내용과 완전히 다른 새로운 내용으로 변경되었습니다. 충분한 길이의 컨텐츠를 포함하고 있습니다."
    }

    @Test
    fun `PUT posts - 태그를 추가하여 게시글을 수정할 수 있어야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser(role = UserRole.ADMIN)
        val accessToken = jwtTokenManager.generateAccessToken(user.id).token

        // Given - 태그 생성
        createTestTag("kotlin", 10)
        createTestTag("spring", 8)
        createTestTag("java", 5)

        // 태그 없는 게시글 생성
        val originalPost =
            createTestPost(
                title = "태그 없는 게시글",
                body = "태그가 없는 게시글입니다. 수정을 통해 태그를 추가할 예정입니다. 최소 30자 이상의 내용이 필요합니다.",
                status = PostStatus.REVIEWED,
                tags = emptySet(),
            )

        val requestBody =
            """
            {
                "title": "태그가 추가된 게시글",
                "body": "태그가 추가된 게시글입니다. kotlin과 spring 태그가 포함되었습니다. 충분한 길이의 컨텐츠를 포함하고 있습니다.",
                "tags": ["kotlin", "spring"]
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}", originalPost.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
            .andDo(
                documentWithResource(
                    "게시글 수정 - 태그 추가",
                    builder()
                        .tag("Post")
                        .summary("게시글 수정 - 태그 추가")
                        .description(
                            """
                            기존 게시글에 태그를 추가합니다.
                            
                            태그는 미리 생성되어 있어야 하며,
                            존재하지 않는 태그를 지정하면 새로 생성됩니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("수정할 게시글 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 본문"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("추가할 태그 목록"),
                        )
                        .build(),
                ),
            )

        // 수정된 내용 확인
        val updatedPost = postRepository.findBy(originalPost.id)
        updatedPost shouldNotBe null
        updatedPost?.tags?.size shouldBe 2
        updatedPost?.tags?.map { tag -> tag.tagName.value }?.toSet() shouldBe setOf("kotlin", "spring")
    }

    @Test
    fun `PUT posts - 태그를 변경하여 게시글을 수정할 수 있어야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser(role = UserRole.ADMIN)
        val accessToken = jwtTokenManager.generateAccessToken(user.id).token

        // Given - 태그 생성
        createTestTag("kotlin", 10)
        createTestTag("spring", 8)
        createTestTag("java", 5)
        createTestTag("mysql", 3)

        // 태그가 있는 게시글 생성
        val originalPost =
            createTestPost(
                title = "기존 태그가 있는 게시글",
                body = "kotlin과 spring 태그가 있는 게시글입니다. 수정을 통해 태그를 변경할 예정입니다.",
                status = PostStatus.REVIEWED,
                tags =
                    setOf(
                        PostTag(TagName("kotlin"), 0),
                        PostTag(TagName("spring"), 1),
                    ),
            )

        val requestBody =
            """
            {
                "title": "태그가 변경된 게시글",
                "body": "태그가 변경된 게시글입니다. java와 mysql 태그로 변경되었습니다. 충분한 길이의 컨텐츠를 포함하고 있습니다.",
                "tags": ["java", "mysql"]
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}", originalPost.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
            .andDo(
                documentWithResource(
                    "게시글 수정 - 태그 변경",
                    builder()
                        .tag("Post")
                        .summary("게시글 수정 - 태그 변경")
                        .description(
                            """
                            게시글의 태그를 다른 태그로 변경합니다.
                            
                            기존 태그는 모두 제거되고 새로운 태그로 교체됩니다.
                            태그를 부분적으로 수정하려면 기존 태그와 새 태그를 모두 포함해야 합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("수정할 게시글 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 본문"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("변경할 태그 목록"),
                        )
                        .build(),
                ),
            )

        // 수정된 내용 확인
        val updatedPost = postRepository.findBy(originalPost.id)
        updatedPost shouldNotBe null
        updatedPost?.tags?.size shouldBe 2
        updatedPost?.tags?.map { tag -> tag.tagName.value }?.toSet() shouldBe setOf("java", "mysql")
    }

    @Test
    fun `PUT posts - 태그를 모두 제거하여 게시글을 수정할 수 있어야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser(role = UserRole.ADMIN)
        val accessToken = jwtTokenManager.generateAccessToken(user.id).token

        // Given - 태그 생성
        createTestTag("kotlin", 10)
        createTestTag("spring", 8)

        // 태그가 있는 게시글 생성
        val originalPost =
            createTestPost(
                title = "태그가 있는 게시글",
                body = "kotlin과 spring 태그가 있는 게시글입니다. 수정을 통해 태그를 모두 제거할 예정입니다.",
                status = PostStatus.REVIEWED,
                tags =
                    setOf(
                        PostTag(TagName("kotlin"), 0),
                        PostTag(TagName("spring"), 1),
                    ),
            )

        val requestBody =
            """
            {
                "title": "태그가 제거된 게시글",
                "body": "태그가 모두 제거된 게시글입니다. 더 이상 태그가 없습니다. 충분한 길이의 컨텐츠를 포함하고 있습니다.",
                "tags": []
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}", originalPost.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
            .andDo(
                documentWithResource(
                    "게시글 수정 - 태그 제거",
                    builder()
                        .tag("Post")
                        .summary("게시글 수정 - 태그 모두 제거")
                        .description(
                            """
                            게시글의 모든 태그를 제거합니다.
                            
                            tags 필드에 빈 배열을 전달하면 모든 태그가 제거됩니다.
                            tags 필드를 생략하면 기존 태그가 유지됩니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("수정할 게시글 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 본문"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("빈 배열 (태그 제거)"),
                        )
                        .build(),
                ),
            )

        // 수정된 내용 확인
        val updatedPost = postRepository.findBy(originalPost.id)
        updatedPost shouldNotBe null
        updatedPost?.tags?.size shouldBe 0
    }

    @Test
    fun `PUT posts - 존재하지 않는 게시글 ID로 수정 시 404를 반환해야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser(role = UserRole.ADMIN)
        val accessToken = jwtTokenManager.generateAccessToken(user.id).token

        // Given - 존재하지 않는 게시글 ID
        val nonExistentPostId = 999999L

        val requestBody =
            """
            {
                "title": "수정할 제목",
                "body": "수정할 본문입니다. 존재하지 않는 게시글을 수정하려고 시도합니다. 최소 30자 이상의 내용이 필요합니다."
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}", nonExistentPostId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("POST_NOT_FOUND"))
            .andDo(
                documentWithResource(
                    "게시글 수정 - 존재하지 않는 게시글",
                    builder()
                        .tag("Post")
                        .summary("게시글 수정 - 404 Not Found")
                        .description(
                            """
                            존재하지 않는 게시글 ID로 수정을 시도하면 404 Not Found를 반환합니다.
                            
                            POST_NOT_FOUND 에러 코드와 함께 에러 메시지가 반환됩니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("존재하지 않는 게시글 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 본문"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `PUT posts - 제목이 빈 문자열이면 400 Bad Request를 반환해야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser(role = UserRole.ADMIN)
        val accessToken = jwtTokenManager.generateAccessToken(user.id).token

        // Given - 기존 게시글 생성
        val originalPost =
            createTestPost(
                title = "원본 제목",
                body = "원본 본문입니다. 제목이 빈 문자열로 수정을 시도합니다. 최소 30자 이상의 내용이 필요합니다.",
                status = PostStatus.REVIEWED,
            )

        val requestBody =
            """
            {
                "title": "",
                "body": "본문은 정상적인 내용입니다. 하지만 제목이 빈 문자열입니다. 충분한 길이의 컨텐츠를 포함하고 있습니다."
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}", originalPost.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("BLANK_TITLE"))
            .andDo(
                documentWithResource(
                    "게시글 수정 - 빈 제목",
                    builder()
                        .tag("Post")
                        .summary("게시글 수정 - 제목 유효성 검증 실패")
                        .description(
                            """
                            제목이 빈 문자열이면 400 Bad Request를 반환합니다.
                            
                            제목은 최소 1자 이상이어야 합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("수정할 게시글 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("빈 문자열 (유효하지 않음)"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 본문"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `PUT posts - 제목이 255자를 초과하면 400 Bad Request를 반환해야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser(role = UserRole.ADMIN)
        val accessToken = jwtTokenManager.generateAccessToken(user.id).token

        // Given - 기존 게시글 생성
        val originalPost =
            createTestPost(
                title = "원본 제목",
                body = "원본 본문입니다. 제목이 너무 긴 문자열로 수정을 시도합니다. 최소 30자 이상의 내용이 필요합니다.",
                status = PostStatus.REVIEWED,
            )

        val longTitle = "A".repeat(256)
        val requestBody =
            """
            {
                "title": "$longTitle",
                "body": "본문은 정상적인 내용입니다. 하지만 제목이 너무 깁니다. 충분한 길이의 컨텐츠를 포함하고 있습니다."
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}", originalPost.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("TITLE_TOO_LONG"))
            .andDo(
                documentWithResource(
                    "게시글 수정 - 제목 길이 초과",
                    builder()
                        .tag("Post")
                        .summary("게시글 수정 - 제목 길이 초과")
                        .description(
                            """
                            제목이 255자를 초과하면 400 Bad Request를 반환합니다.
                            
                            제목은 최대 255자까지 허용됩니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("수정할 게시글 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("256자 이상의 제목 (유효하지 않음)"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 본문"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `PUT posts - 본문이 30자 미만이면 400 Bad Request를 반환해야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser(role = UserRole.ADMIN)
        val accessToken = jwtTokenManager.generateAccessToken(user.id).token

        // Given - 기존 게시글 생성
        val originalPost =
            createTestPost(
                title = "원본 제목",
                body = "원본 본문입니다. 본문이 너무 짧은 문자열로 수정을 시도합니다. 최소 30자 이상의 내용이 필요합니다.",
                status = PostStatus.REVIEWED,
            )

        val requestBody =
            """
            {
                "title": "정상적인 제목",
                "body": "너무 짧은 본문"
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}", originalPost.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("CONTENT_TOO_SHORT"))
            .andDo(
                documentWithResource(
                    "게시글 수정 - 본문 길이 부족",
                    builder()
                        .tag("Post")
                        .summary("게시글 수정 - 본문 길이 부족")
                        .description(
                            """
                            본문이 30자 미만이면 400 Bad Request를 반환합니다.
                            
                            본문은 최소 30자 이상이어야 합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("수정할 게시글 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("30자 미만의 본문 (유효하지 않음)"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `PUT posts - 삭제된 게시글도 수정할 수 있다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser(role = UserRole.ADMIN)
        val accessToken = jwtTokenManager.generateAccessToken(user.id).token

        // Given - 삭제된 게시글 생성
        val deletedPost =
            createTestPost(
                title = "삭제된 게시글",
                body = "이미 삭제된 게시글입니다. 수정할 수 없어야 합니다. 최소 30자 이상의 내용이 필요합니다.",
                status = PostStatus.DELETED,
            )

        val requestBody =
            """
            {
                "title": "수정 시도",
                "body": "삭제된 게시글을 수정하려고 시도합니다. 이것은 실패해야 합니다. 충분한 길이의 컨텐츠를 포함하고 있습니다."
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}", deletedPost.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
            .andDo(
                documentWithResource(
                    "게시글 수정 - 삭제된 게시글",
                    builder()
                        .tag("Post")
                        .summary("게시글 수정 - 삭제된 게시글도 가능")
                        .description(
                            """
                            삭제된 게시글(DELETED 상태)도 수정할 수 있습니다.
                            
                            수정 성공 시 204 No Content를 반환합니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("삭제된 게시글 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 본문"),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `PUT posts - 잘못된 태그 이름을 전달하면 400 Bad Request를 반환해야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser(role = UserRole.ADMIN)
        val accessToken = jwtTokenManager.generateAccessToken(user.id).token

        // Given - 기존 게시글 생성
        val originalPost =
            createTestPost(
                title = "원본 제목",
                body = "원본 본문입니다. 잘못된 태그 이름으로 수정을 시도합니다. 최소 30자 이상의 내용이 필요합니다.",
                status = PostStatus.REVIEWED,
            )

        val requestBody =
            """
            {
                "title": "정상적인 제목",
                "body": "정상적인 본문입니다. 하지만 태그 이름이 유효하지 않습니다. 충분한 길이의 컨텐츠를 포함하고 있습니다.",
                "tags": ["", "   ", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"]
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}", originalPost.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andDo(
                documentWithResource(
                    "게시글 수정 - 잘못된 태그",
                    builder()
                        .tag("Post")
                        .summary("게시글 수정 - 잘못된 태그 이름")
                        .description(
                            """
                            잘못된 태그 이름을 전달하면 400 Bad Request를 반환합니다.
                            
                            태그 이름은 1자 이상 50자 이하여야 하며,
                            빈 문자열이나 공백만으로 이루어진 문자열은 허용되지 않습니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("수정할 게시글 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("수정할 게시글 본문"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("잘못된 형식의 태그 목록"),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `PUT posts - 동일한 내용으로 수정해도 정상 처리되어야 한다`() {
        // Given - 사용자 생성 및 인증 토큰 발급
        val user = createTestUser(role = UserRole.ADMIN)
        val accessToken = jwtTokenManager.generateAccessToken(user.id).token

        // Given - 기존 게시글 생성
        val originalPost =
            createTestPost(
                title = "변경되지 않을 제목",
                body = "변경되지 않을 본문입니다. 동일한 내용으로 수정 요청을 보냅니다. 최소 30자 이상의 내용이 필요합니다.",
                status = PostStatus.REVIEWED,
            )

        val requestBody =
            """
            {
                "title": "변경되지 않을 제목",
                "body": "변경되지 않을 본문입니다. 동일한 내용으로 수정 요청을 보냅니다. 최소 30자 이상의 내용이 필요합니다."
            }
            """.trimIndent()

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}", originalPost.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
            .andDo(
                documentWithResource(
                    "게시글 수정 - 동일한 내용",
                    builder()
                        .tag("Post")
                        .summary("게시글 수정 - 동일한 내용으로 수정")
                        .description(
                            """
                            동일한 내용으로 수정 요청을 보내도 정상적으로 처리됩니다.
                            
                            내용이 변경되지 않더라도 수정 작업은 성공하며,
                            버전 번호는 증가할 수 있습니다.
                            """.trimIndent(),
                        )
                        .pathParameters(
                            parameterWithName("postId")
                                .description("수정할 게시글 ID"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("기존과 동일한 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("기존과 동일한 본문"),
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
