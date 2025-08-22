package me.helloc.techwikiplus.post.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters.Companion.builder
import com.epages.restdocs.apispec.Schema.Companion.schema
import me.helloc.techwikiplus.common.config.BaseE2eTest
import me.helloc.techwikiplus.common.config.annotations.E2eTest
import me.helloc.techwikiplus.common.config.documentation.withStandardErrorResponse
import me.helloc.techwikiplus.common.infrastructure.id.SnowflakePostIdGenerator
import me.helloc.techwikiplus.common.infrastructure.security.jwt.JwtTokenManager
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Instant

/**
 * CreatePostController E2E 테스트
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
class CreatePostControllerE2eTest : BaseE2eTest() {
    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var postIdGenerator: SnowflakePostIdGenerator

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenManager: JwtTokenManager

    @Test
    fun `POST posts - 유효한 게시글 데이터로 201 Created를 반환해야 한다`() {
        // Given - ADMIN 사용자 생성
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val request =
            CreatePostController.Request(
                title = "테스트 게시글 제목",
                body = "이것은 테스트 게시글의 본문 내용입니다. 최소 30자 이상의 내용을 포함하고 있습니다.",
                tags = listOf("springboot", "react"),
            )

        // When & Then
        var locationHeader: String? = null

        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
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
                    "게시글 생성 성공",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 생성")
                        .description(
                            """
                            새로운 게시글을 생성합니다.
                            
                            게시글이 성공적으로 생성되면 201 Created 상태 코드와 함께
                            Location 헤더에 생성된 게시글의 URI가 반환됩니다.
                            생성된 게시글은 기본적으로 DRAFT 상태로 저장됩니다.
                            """.trimIndent(),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("게시글 제목 (최대 150자, 빈 값 불가)"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("게시글 본문 (최소 30자, 최대 50000자)"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 태그 (선택 사항, 최대 10개, 각 태그는 최대 30자)"),
                        )
                        .responseHeaders(
                            headerWithName(HttpHeaders.LOCATION)
                                .description("생성된 게시글의 URI"),
                        )
                        .requestSchema(
                            schema(
                                "${CreatePostController::class.simpleName}" +
                                    ".${CreatePostController.Request::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )

        // Then - DB 저장 확인
        val postId = locationHeader?.substringAfterLast("/")?.toLongOrNull()
        postId shouldNotBe null

        val savedPost = postRepository.findBy(PostId(postId!!))
        savedPost shouldNotBe null
        savedPost!!.title.value shouldBe request.title
        savedPost.body.value shouldBe request.body
        savedPost.status shouldBe PostStatus.DRAFT
    }

    @Test
    fun `POST posts - 제목이 비어있는 경우 400 Bad Request를 반환해야 한다`() {
        // Given - ADMIN 사용자 생성
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val request =
            CreatePostController.Request(
                title = "",
                body = "이것은 테스트 게시글의 본문 내용입니다. 최소 30자 이상의 내용을 포함하고 있습니다.",
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("BLANK_TITLE"))
            .andDo(
                documentWithResource(
                    "빈 제목으로 게시글 생성",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 생성 - 빈 제목")
                        .description("제목이 비어있는 경우 400 Bad Request를 반환합니다.")
                        .requestSchema(
                            schema(
                                "${CreatePostController::class.simpleName}" +
                                    ".${CreatePostController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST posts - 제목이 150자를 초과하는 경우 400 Bad Request를 반환해야 한다`() {
        // Given - ADMIN 사용자 생성
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val longTitle = "가".repeat(151)
        val request =
            CreatePostController.Request(
                title = longTitle,
                body = "이것은 테스트 게시글의 본문 내용입니다. 최소 30자 이상의 내용을 포함하고 있습니다.",
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("TITLE_TOO_LONG"))
            .andDo(
                documentWithResource(
                    "너무 긴 제목으로 게시글 생성",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 생성 - 제목 길이 초과")
                        .description("제목이 150자를 초과하는 경우 400 Bad Request를 반환합니다.")
                        .requestSchema(
                            schema(
                                "${CreatePostController::class.simpleName}" +
                                    ".${CreatePostController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST posts - 본문이 비어있는 경우 400 Bad Request를 반환해야 한다`() {
        // Given - ADMIN 사용자 생성
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val request =
            CreatePostController.Request(
                title = "테스트 게시글 제목",
                body = "",
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("BLANK_CONTENT"))
            .andDo(
                documentWithResource(
                    "빈 본문으로 게시글 생성",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 생성 - 빈 본문")
                        .description("본문이 비어있는 경우 400 Bad Request를 반환합니다.")
                        .requestSchema(
                            schema(
                                "${CreatePostController::class.simpleName}" +
                                    ".${CreatePostController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST posts - 본문이 30자 미만인 경우 400 Bad Request를 반환해야 한다`() {
        // Given - ADMIN 사용자 생성
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val request =
            CreatePostController.Request(
                title = "테스트 게시글 제목",
                body = "짧은 내용",
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("CONTENT_TOO_SHORT"))
            .andDo(
                documentWithResource(
                    "너무 짧은 본문으로 게시글 생성",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 생성 - 본문 길이 부족")
                        .description("본문이 30자 미만인 경우 400 Bad Request를 반환합니다.")
                        .requestSchema(
                            schema(
                                "${CreatePostController::class.simpleName}" +
                                    ".${CreatePostController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST posts - 본문이 50000자를 초과하는 경우 400 Bad Request를 반환해야 한다`() {
        // Given - ADMIN 사용자 생성
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val longBody = "가".repeat(50001)
        val request =
            CreatePostController.Request(
                title = "테스트 게시글 제목",
                body = longBody,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("CONTENT_TOO_LONG"))
            .andDo(
                documentWithResource(
                    "너무 긴 본문으로 게시글 생성",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 생성 - 본문 길이 초과")
                        .description("본문이 50000자를 초과하는 경우 400 Bad Request를 반환합니다.")
                        .requestSchema(
                            schema(
                                "${CreatePostController::class.simpleName}" +
                                    ".${CreatePostController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST posts - 제목에 특수문자가 포함된 경우에도 정상 생성되어야 한다`() {
        // Given - ADMIN 사용자 생성
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val request =
            CreatePostController.Request(
                title = "Spring Boot 3.0 & Kotlin 1.9 - 새로운 기능들!",
                body = "Spring Boot 3.0과 Kotlin 1.9의 새로운 기능들을 소개합니다. 이 버전에서는 많은 개선사항이 있습니다.",
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.LOCATION))
    }

    @Test
    fun `POST posts - 동일한 제목으로 여러 게시글을 생성할 수 있어야 한다`() {
        // Given - ADMIN 사용자 생성
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val sameTitle = "중복 가능한 제목"
        val request1 =
            CreatePostController.Request(
                title = sameTitle,
                body = "첫 번째 게시글의 내용입니다. 제목은 같지만 내용은 다릅니다.",
            )
        val request2 =
            CreatePostController.Request(
                title = sameTitle,
                body = "두 번째 게시글의 내용입니다. 제목은 같지만 내용이 완전히 다릅니다.",
            )

        // When - 첫 번째 게시글 생성
        var locationHeader1: String? = null
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andDo { result ->
                locationHeader1 = result.response.getHeader(HttpHeaders.LOCATION)
            }

        // When - 두 번째 게시글 생성
        var locationHeader2: String? = null
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andDo { result ->
                locationHeader2 = result.response.getHeader(HttpHeaders.LOCATION)
            }

        // Then - 두 게시글이 서로 다른 ID를 가져야 함
        val postId1 = locationHeader1?.substringAfterLast("/")?.toLongOrNull()
        val postId2 = locationHeader2?.substringAfterLast("/")?.toLongOrNull()

        postId1 shouldNotBe null
        postId2 shouldNotBe null
        postId1 shouldNotBe postId2

        val post1 = postRepository.findBy(PostId(postId1!!))
        val post2 = postRepository.findBy(PostId(postId2!!))

        post1?.title?.value shouldBe sameTitle
        post2?.title?.value shouldBe sameTitle
        post1?.body?.value shouldBe request1.body
        post2?.body?.value shouldBe request2.body
    }

    @Test
    fun `POST posts - 연속으로 여러 게시글을 생성할 수 있어야 한다`() {
        // Given - ADMIN 사용자 생성
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val requests =
            (1..5).map { i ->
                CreatePostController.Request(
                    title = "연속 생성 테스트 게시글 $i",
                    body = "이것은 연속 생성 테스트를 위한 게시글 번호 $i 의 본문 내용입니다.",
                )
            }

        // When & Then
        val locationHeaders = mutableListOf<String>()

        requests.forEach { request ->
            mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/posts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(MockMvcResultMatchers.status().isCreated)
                .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.LOCATION))
                .andDo { result ->
                    result.response.getHeader(HttpHeaders.LOCATION)?.let {
                        locationHeaders.add(it)
                    }
                }
        }

        // Then - 모든 게시글이 다른 ID를 가져야 함
        val postIds =
            locationHeaders.mapNotNull {
                it.substringAfterLast("/").toLongOrNull()
            }

        postIds.size shouldBe 5
        postIds.toSet().size shouldBe 5 // 모든 ID가 고유함

        // DB에 모든 게시글이 저장되었는지 확인
        postIds.forEachIndexed { index, id ->
            val post = postRepository.findBy(PostId(id))
            post shouldNotBe null
            post?.title?.value shouldBe "연속 생성 테스트 게시글 ${index + 1}"
        }
    }

    @Test
    fun `POST posts - ADMIN 권한이 있는 사용자만 게시글을 생성할 수 있다`() {
        // Given - ADMIN 사용자 생성
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val request =
            CreatePostController.Request(
                title = "ADMIN이 작성한 게시글",
                body = "ADMIN 권한을 가진 사용자가 작성한 게시글입니다. 정상적으로 생성되어야 합니다.",
                tags = listOf("springboot", "react"),
            )

        // When & Then
        var locationHeader: String? = null

        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
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
                    "ADMIN 권한으로 게시글 생성 성공",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 생성 - ADMIN 권한")
                        .description(
                            """
                            ADMIN 권한을 가진 사용자가 게시글을 생성합니다.
                            
                            게시글 생성은 ADMIN 권한을 가진 사용자만 가능합니다.
                            Authorization 헤더에 유효한 JWT 토큰이 필요하며,
                            해당 토큰의 사용자가 ADMIN 권한을 가지고 있어야 합니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰}"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("게시글 본문"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 태그"),
                        )
                        .responseHeaders(
                            headerWithName(HttpHeaders.LOCATION)
                                .description("생성된 게시글의 URI"),
                        )
                        .requestSchema(
                            schema(
                                "${CreatePostController::class.simpleName}" +
                                    ".${CreatePostController.Request::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )

        // Then - DB 저장 확인
        val postId = locationHeader?.substringAfterLast("/")?.toLongOrNull()
        postId shouldNotBe null

        val savedPost = postRepository.findBy(PostId(postId!!))
        savedPost shouldNotBe null
        savedPost!!.title.value shouldBe request.title
    }

    @Test
    fun `POST posts - USER 권한만 있는 사용자는 게시글을 생성할 수 없다`() {
        // Given - 일반 USER 생성
        val normalUser = createTestUser(role = UserRole.USER)
        val userToken = jwtTokenManager.generateAccessToken(normalUser.id).token

        val request =
            CreatePostController.Request(
                title = "일반 사용자가 시도한 게시글",
                body = "일반 사용자가 작성을 시도한 게시글입니다. 권한이 없어 실패해야 합니다.",
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("UNAUTHORIZED_ACCESS"))
            .andDo(
                documentWithResource(
                    "USER 권한으로 게시글 생성 실패",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 생성 - 권한 부족")
                        .description(
                            """
                            USER 권한만 가진 사용자가 게시글 생성을 시도하면 403 Forbidden을 반환합니다.
                            
                            게시글 생성은 ADMIN 권한이 필요하므로,
                            일반 USER는 게시글을 생성할 수 없습니다.
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰}"),
                        )
                        .requestSchema(
                            schema(
                                "${CreatePostController::class.simpleName}" +
                                    ".${CreatePostController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST posts - 인증되지 않은 사용자는 게시글을 생성할 수 없다`() {
        // Given - 인증 헤더 없이 요청
        val request =
            CreatePostController.Request(
                title = "인증되지 않은 사용자의 게시글",
                body = "인증 없이 게시글을 작성하려는 시도입니다. 401 응답을 받아야 합니다.",
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andDo(
                documentWithResource(
                    "인증 없이 게시글 생성 시도",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 생성 - 인증 필요")
                        .description(
                            """
                            인증되지 않은 사용자가 게시글 생성을 시도하면 403 Forbidden를 반환합니다.
                            
                            게시글 생성은 관리자 계정만 가능합니다.
                            """.trimIndent(),
                        )
                        .requestSchema(
                            schema(
                                "${CreatePostController::class.simpleName}" +
                                    ".${CreatePostController.Request::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    // 동시성 테스트는 CreatePostConcurrentE2eTest로 이동됨
    // @Transactional이 없는 환경에서 실행되어야 올바른 동시성 검증이 가능

    /* 이동됨: `POST posts - 동시에 여러 사용자가 같은 태그로 게시글을 생성할 수 있어야 한다`
        // 동시성 테스트 모범 사례:
        // 1. CyclicBarrier를 사용하여 모든 스레드가 동시에 시작하도록 보장
        // 2. CompletableFuture로 비동기 작업 처리
        // 3. 명확한 타임아웃 설정으로 데드락 방지
        // 4. 자원 정리 (executor shutdown)

        // Given - 여러 ADMIN 사용자 생성
        val concurrentUsers = 10
        val adminUsers =
            (1..concurrentUsers).map { index ->
                createTestUser(
                    email = "concurrent_admin_$index@test.com",
                    nickname = "ConcurrentAdmin$index",
                    role = UserRole.ADMIN,
                )
            }
        val adminTokens =
            adminUsers.map { user ->
                jwtTokenManager.generateAccessToken(user.id).token
            }

        // 동일한 태그 사용 - 분산 락 테스트를 위해
        val sharedTags = listOf("concurrent", "test", "spring")

        // 동시성 제어를 위한 CyclicBarrier
        val startBarrier = CyclicBarrier(concurrentUsers + 1) // +1 for main thread
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val results = mutableListOf<CompletableFuture<Pair<Boolean, String?>>>()

        try {
            // When - 동시 요청 시작
            adminTokens.forEachIndexed { index, token ->
                val future =
                    CompletableFuture.supplyAsync(
                        {
                            try {
                                startBarrier.await(10, TimeUnit.SECONDS) // 동기화 지점

                                val request =
                                    CreatePostController.Request(
                                        title = "동시성 테스트 ${index + 1}",
                                        body = "동시 생성 테스트입니다. 사용자 ${index + 1}이 작성했습니다. 최소 30자 이상의 내용을 포함합니다.",
                                        tags = sharedTags,
                                    )

                                var locationHeader: String? = null
                                mockMvc.perform(
                                    RestDocumentationRequestBuilders.post("/api/v1/posts")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)),
                                )
                                    .andExpect(MockMvcResultMatchers.status().isCreated)
                                    .andDo { result ->
                                        locationHeader = result.response.getHeader(HttpHeaders.LOCATION)
                                    }

                                Pair(true, locationHeader)
                            } catch (e: Exception) {
                                println("Error in thread ${index + 1}: ${e.message}")
                                Pair(false, e.message)
                            }
                        },
                        executor,
                    )
                results.add(future)
            }

            // 모든 스레드가 준비되면 동시에 시작
            startBarrier.await(10, TimeUnit.SECONDS)

            // Then - 결과 검증
            val completedResults = results.map { it.get(30, TimeUnit.SECONDS) }

            // 모든 요청이 성공해야 함
            val successCount = completedResults.count { it.first }
            successCount shouldBe concurrentUsers

            // 생성된 게시글 ID 추출 및 검증
            val postIds =
                completedResults
                    .filter { it.first }
                    .mapNotNull { it.second?.substringAfterLast("/")?.toLongOrNull() }

            // 모든 ID가 고유해야 함
            postIds.size shouldBe concurrentUsers
            postIds.toSet().size shouldBe concurrentUsers

            // 각 게시글의 태그 확인
            postIds.forEach { id ->
                val post = postRepository.findBy(PostId(id))
                post shouldNotBe null
                post?.tags?.map { it.tagName.value }?.toSet() shouldBe sharedTags.toSet()
            }
        } finally {
            // 자원 정리
            executor.shutdown()
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        }
    } */

    /* 이동됨: `POST posts - Snowflake ID Generator는 동시 요청에서도 고유한 ID를 생성해야 한다`
        // 동시성 테스트 모범 사례:
        // 1. CountDownLatch로 정확한 동시 시작 제어
        // 2. 충분한 수의 동시 요청으로 ID 충돌 가능성 테스트
        // 3. Snowflake ID의 순서 보장 검증

        // Given
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val concurrentRequests = 50 // 충분히 많은 동시 요청
        val startLatch = CountDownLatch(1)
        val readyLatch = CountDownLatch(concurrentRequests)
        val executor = Executors.newFixedThreadPool(concurrentRequests)
        val futures = mutableListOf<CompletableFuture<Long?>>()

        try {
            // When - 동시 요청 준비
            repeat(concurrentRequests) { index ->
                val future =
                    CompletableFuture.supplyAsync(
                        {
                            try {
                                readyLatch.countDown() // 준비 완료 신호
                                startLatch.await(10, TimeUnit.SECONDS) // 시작 신호 대기

                                val request =
                                    CreatePostController.Request(
                                        title = "Snowflake ID 테스트 $index",
                                        body = "동시에 생성되는 게시글입니다. Snowflake ID의 고유성을 테스트합니다. 최소 30자 이상.",
                                    )

                                var locationHeader: String? = null
                                mockMvc.perform(
                                    RestDocumentationRequestBuilders.post("/api/v1/posts")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)),
                                )
                                    .andExpect(MockMvcResultMatchers.status().isCreated)
                                    .andDo { result ->
                                        locationHeader = result.response.getHeader(HttpHeaders.LOCATION)
                                    }

                                locationHeader?.substringAfterLast("/")?.toLongOrNull()
                            } catch (e: Exception) {
                                println("Error in request $index: ${e.message}")
                                null
                            }
                        },
                        executor,
                    )
                futures.add(future)
            }

            // 모든 스레드가 준비될 때까지 대기
            readyLatch.await(10, TimeUnit.SECONDS)

            // 동시에 모든 요청 시작
            startLatch.countDown()

            // Then - 결과 검증
            val postIds = futures.mapNotNull { it.get(30, TimeUnit.SECONDS) }

            // 모든 요청이 성공해야 함
            postIds.size shouldBe concurrentRequests

            // 모든 ID가 고유해야 함 (중복 방지)
            val uniqueIds = postIds.toSet()
            uniqueIds.size shouldBe concurrentRequests

            // Snowflake ID의 특성: 시간 순서 보장
            // 정렬했을 때 모든 ID가 증가 순서여야 함
            val sortedIds = postIds.sorted()
            sortedIds.zipWithNext().all { (prev, next) -> prev < next } shouldBe true

            // ID 범위 검증 (모두 양수여야 함)
            postIds.all { it > 0 } shouldBe true
        } finally {
            executor.shutdown()
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        }
    } */

    /* 이동됨: `POST posts - 동시에 같은 제목으로 게시글을 생성해도 모두 성공해야 한다`
        // 동시성 테스트 모범 사례:
        // 제목 중복이 허용되는지 검증
        // 비지니스 로직상 제목 중복은 허용되어야 함

        // Given
        val userCount = 5
        val adminUsers =
            (1..userCount).map { index ->
                createTestUser(
                    email = "same_title_test_$index@test.com",
                    nickname = "SameTitleAdmin$index",
                    role = UserRole.ADMIN,
                )
            }
        val adminTokens =
            adminUsers.map { user ->
                jwtTokenManager.generateAccessToken(user.id).token
            }

        val sharedTitle = "동시에 생성되는 동일한 제목"
        val barrier = CyclicBarrier(userCount)
        val executor = Executors.newFixedThreadPool(userCount)
        val futures = mutableListOf<CompletableFuture<Long?>>()

        try {
            // When
            adminTokens.forEachIndexed { index, token ->
                val future =
                    CompletableFuture.supplyAsync(
                        {
                            try {
                                barrier.await(10, TimeUnit.SECONDS)

                                val request =
                                    CreatePostController.Request(
                                        title = sharedTitle,
                                        body = "사용자 ${index + 1}이 작성한 고유한 내용입니다. 제목은 동일하지만 내용은 다릅니다. 최소 30자.",
                                    )

                                var locationHeader: String? = null
                                mockMvc.perform(
                                    RestDocumentationRequestBuilders.post("/api/v1/posts")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)),
                                )
                                    .andExpect(MockMvcResultMatchers.status().isCreated)
                                    .andDo { result ->
                                        locationHeader = result.response.getHeader(HttpHeaders.LOCATION)
                                    }

                                locationHeader?.substringAfterLast("/")?.toLongOrNull()
                            } catch (e: Exception) {
                                println("Error for user ${index + 1}: ${e.message}")
                                null
                            }
                        },
                        executor,
                    )
                futures.add(future)
            }

            // Then
            val postIds = futures.mapNotNull { it.get(30, TimeUnit.SECONDS) }

            // 모든 게시글이 성공적으로 생성되어야 함
            postIds.size shouldBe userCount

            // 각 게시글은 고유한 ID를 가져야 함
            postIds.toSet().size shouldBe userCount

            // 모든 게시글이 동일한 제목을 가져야 함
            postIds.forEach { id ->
                val post = postRepository.findBy(PostId(id))
                post shouldNotBe null
                post?.title?.value shouldBe sharedTitle
            }
        } finally {
            executor.shutdown()
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        }
    } */

    @Test
    fun `POST posts - 중복된 태그는 자동으로 제거되어야 한다`() {
        // Given - ADMIN 사용자 생성
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val request =
            CreatePostController.Request(
                title = "중복 태그 테스트 게시글",
                body = "중복된 태그를 포함한 게시글입니다. 중복된 태그는 자동으로 제거되어야 합니다.",
                tags = listOf("kotlin", "spring", "kotlin", "java", "spring", "kotlin", "java"),
            )

        // When & Then
        var locationHeader: String? = null

        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
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
                    "중복 태그 제거하여 게시글 생성",
                    builder()
                        .tag("Post Management")
                        .summary("게시글 생성 - 중복 태그 제거")
                        .description(
                            """
                            중복된 태그를 포함하여 게시글을 생성하면 중복이 자동으로 제거됩니다.
                            
                            예시: ["kotlin", "spring", "kotlin", "java", "spring", "kotlin", "java"]
                            결과: ["kotlin", "spring", "java"] (중복 제거됨)
                            """.trimIndent(),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION)
                                .description("Bearer {JWT 토큰}"),
                        )
                        .requestFields(
                            fieldWithPath("title")
                                .type(JsonFieldType.STRING)
                                .description("게시글 제목"),
                            fieldWithPath("body")
                                .type(JsonFieldType.STRING)
                                .description("게시글 본문"),
                            fieldWithPath("tags")
                                .type(JsonFieldType.ARRAY)
                                .description("게시글 태그 (중복 포함 가능)"),
                        )
                        .responseHeaders(
                            headerWithName(HttpHeaders.LOCATION)
                                .description("생성된 게시글의 URI"),
                        )
                        .requestSchema(
                            schema(
                                "${CreatePostController::class.simpleName}" +
                                    ".${CreatePostController.Request::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )

        // Then - DB에 저장된 태그에서 중복이 제거되었는지 확인
        val postId = locationHeader?.substringAfterLast("/")?.toLongOrNull()
        postId shouldNotBe null

        val savedPost = postRepository.findBy(PostId(postId!!))
        savedPost shouldNotBe null
        
        // 중복이 제거된 태그 확인
        val savedTags = savedPost!!.tags.map { it.tagName.value }.toSet()
        savedTags shouldBe setOf("kotlin", "spring", "java")
        
        // 태그 개수 확인 (중복 제거 후 3개여야 함)
        savedPost.tags.size shouldBe 3
    }

    @Test
    fun `POST posts - 제목과 본문의 앞뒤 공백은 자동으로 제거되어야 한다`() {
        // Given - ADMIN 사용자 생성
        val adminUser = createTestUser(role = UserRole.ADMIN)
        val adminToken = jwtTokenManager.generateAccessToken(adminUser.id).token

        val request =
            CreatePostController.Request(
                title = "  앞뒤 공백이 있는 제목  ",
                body = "   앞뒤 공백이 있는 본문 내용입니다. 이 공백들은 자동으로 제거되어야 합니다.   ",
            )

        // When & Then
        var locationHeader: String? = null

        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/v1/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andDo { result ->
                locationHeader = result.response.getHeader(HttpHeaders.LOCATION)
            }

        // Then - DB에 저장된 값에서 공백이 제거되었는지 확인
        val postId = locationHeader?.substringAfterLast("/")?.toLongOrNull()
        postId shouldNotBe null

        val savedPost = postRepository.findBy(PostId(postId!!))
        savedPost shouldNotBe null
        savedPost!!.title.value shouldBe "앞뒤 공백이 있는 제목"
        savedPost.body.value shouldBe "앞뒤 공백이 있는 본문 내용입니다. 이 공백들은 자동으로 제거되어야 합니다."
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
        // Snowflake ID Generator를 사용하여 고유한 ID 생성
        val userId = UserId(System.nanoTime())
        val user =
            User(
                id = userId,
                email = Email(email),
                nickname = Nickname(nickname),
                // No-op encoded password for test
                encodedPassword = EncodedPassword("{noop}password123!"),
                status = UserStatus.ACTIVE,
                role = role,
                createdAt = now,
                updatedAt = now,
            )

        // 테스트 데이터베이스에 사용자 저장
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
