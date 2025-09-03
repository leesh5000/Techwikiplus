package me.helloc.techwikiplus.post.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters.Companion.builder
import com.epages.restdocs.apispec.Schema.Companion.schema
import me.helloc.techwikiplus.common.config.BaseE2eTest
import me.helloc.techwikiplus.common.config.annotations.E2eTest
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
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Instant

/**
 * DeletePostController E2E 테스트
 *
 * - 전체 애플리케이션 컨텍스트 로드
 * - TestContainers를 통한 실제 DB 연동
 * - 운영 환경과 동일한 설정
 * - End-to-End 검증
 * - API 문서 자동 생성 (generateDocs = true)
 * - Soft Delete 방식으로 구현
 */
@E2eTest(generateDocs = true)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-post",
        "spring.application.version=1.0.0-INTEGRATION",
        "api.documentation.enabled=true",
    ],
)
class DeletePostControllerE2eTest : BaseE2eTest() {
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
    fun `DELETE posts-postId - 관리자가 존재하는 게시글을 삭제하면 204 NO_CONTENT를 반환해야 한다`() {
        // Given - 관리자 계정 생성
        val admin = createAdminUser()
        val adminToken = jwtTokenManager.generateAccessToken(admin.id).token

        // Given - 테스트 게시글 생성
        val post =
            createTestPost(
                title = "삭제할 게시글 제목",
                body = "삭제할 게시글 본문입니다. 이 게시글은 곧 삭제될 예정입니다.",
                status = PostStatus.REVIEWED,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.delete("/api/v1/posts/{postId}", post.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent)
            .andDo(
                documentWithResource(
                    "delete-post",
                    builder()
                        .tag("Post")
                        .summary("게시글 삭제")
                        .description("게시글을 삭제합니다. (Soft Delete)")
                        .pathParameters(
                            parameterWithName("postId").description("삭제할 게시글 ID"),
                        )
                        .requestHeaders(
                            headerWithName(HttpHeaders.AUTHORIZATION).description("JWT 인증 토큰"),
                        )
                        .responseSchema(schema("empty"))
                        .build(),
                ),
            )

        // 삭제 후 게시글 상태 확인
        val deletedPost = postRepository.findBy(post.id)
        assert(deletedPost != null)
        assert(deletedPost?.status == PostStatus.DELETED)
    }

    @Test
    fun `DELETE posts-postId - 태그가 있는 게시글을 삭제해도 태그 카운트는 유지되어야 한다 (Soft Delete)`() {
        // Given - 관리자 계정 생성
        val admin = createAdminUser()
        val adminToken = jwtTokenManager.generateAccessToken(admin.id).token

        // Given - 태그 생성
        val kotlinTag = createTestTag("kotlin", 5)
        val springTag = createTestTag("spring", 3)

        // Given - 태그가 있는 게시글 생성
        val post =
            createTestPost(
                title = "Kotlin과 Spring 게시글",
                body = "Kotlin과 Spring Boot를 사용한 개발 경험을 공유합니다.",
                status = PostStatus.REVIEWED,
                tags =
                    setOf(
                        PostTag(TagName("kotlin"), 0),
                        PostTag(TagName("spring"), 1),
                    ),
            )

        // When
        mockMvc.perform(
            RestDocumentationRequestBuilders.delete("/api/v1/posts/{postId}", post.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent)

        // Then - 태그 카운트는 변경되지 않음 (Soft Delete이므로)
        val updatedKotlinTag = tagJpaRepository.findById(kotlinTag.id).orElse(null)
        val updatedSpringTag = tagJpaRepository.findById(springTag.id).orElse(null)

        assert(updatedKotlinTag.postCount == 5) // 변경 없음
        assert(updatedSpringTag.postCount == 3) // 변경 없음

        // 게시글은 DELETED 상태로 변경됨
        val deletedPost = postRepository.findBy(post.id)
        assert(deletedPost?.status == PostStatus.DELETED)
    }

    @Test
    fun `DELETE posts-postId - 일반 사용자가 게시글 삭제를 시도하면 403 FORBIDDEN을 반환해야 한다`() {
        // Given - 일반 사용자 생성
        val user = createTestUser()
        val userToken = jwtTokenManager.generateAccessToken(user.id).token

        // Given - 테스트 게시글 생성
        val post =
            createTestPost(
                title = "삭제 권한 테스트 게시글",
                body = "일반 사용자는 이 게시글을 삭제할 수 없습니다. 이 테스트는 권한 검증을 위한 것입니다.",
                status = PostStatus.REVIEWED,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.delete("/api/v1/posts/{postId}", post.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $userToken")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("FORBIDDEN_POST_ROLE"))

        // 게시글이 삭제되지 않았는지 확인
        val notDeletedPost = postRepository.findBy(post.id)
        assert(notDeletedPost?.status == PostStatus.REVIEWED)
    }

    @Test
    fun `DELETE posts-postId - 인증되지 않은 사용자가 게시글 삭제를 시도하면 403 FORBIDDEN을 반환해야 한다`() {
        // Given - 테스트 게시글 생성
        val post =
            createTestPost(
                title = "인증 테스트 게시글",
                body = "인증되지 않은 사용자는 이 게시글을 삭제할 수 없습니다. 인증이 필요한 작업입니다.",
                status = PostStatus.REVIEWED,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.delete("/api/v1/posts/{postId}", post.id.value)
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `DELETE posts-postId - 존재하지 않는 게시글을 삭제하면 404 NOT_FOUND를 반환해야 한다`() {
        // Given - 관리자 계정 생성
        val admin = createAdminUser()
        val adminToken = jwtTokenManager.generateAccessToken(admin.id).token

        val nonExistentPostId = 999999L

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.delete("/api/v1/posts/{postId}", nonExistentPostId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("POST_NOT_FOUND"))
    }

    @Test
    fun `DELETE posts-postId - 이미 삭제된 게시글을 다시 삭제하면 410 Gone를 반환해야 한다`() {
        // Given - 관리자 계정 생성
        val admin = createAdminUser()
        val adminToken = jwtTokenManager.generateAccessToken(admin.id).token

        // Given - 이미 삭제된 게시글 생성
        val post =
            createTestPost(
                title = "이미 삭제된 게시글",
                body = "이 게시글은 이미 삭제된 상태입니다. 중복 삭제는 허용되지 않습니다.",
                status = PostStatus.DELETED,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.delete("/api/v1/posts/{postId}", post.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isGone)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("POST_DELETED"))
    }

    @Test
    fun `DELETE posts-postId - DRAFT 상태의 게시글도 삭제할 수 있어야 한다`() {
        // Given - 관리자 계정 생성
        val admin = createAdminUser()
        val adminToken = jwtTokenManager.generateAccessToken(admin.id).token

        // Given - DRAFT 상태 게시글 생성
        val post =
            createTestPost(
                title = "초안 게시글",
                body = "아직 작성 중인 초안 게시글입니다. 이 게시글은 삭제가 가능해야 합니다.",
                status = PostStatus.DRAFT,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.delete("/api/v1/posts/{postId}", post.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent)

        // 삭제 확인
        val deletedPost = postRepository.findBy(post.id)
        assert(deletedPost?.status == PostStatus.DELETED)
    }

    @Test
    fun `DELETE posts-postId - IN_REVIEW 상태의 게시글도 삭제할 수 있어야 한다`() {
        // Given - 관리자 계정 생성
        val admin = createAdminUser()
        val adminToken = jwtTokenManager.generateAccessToken(admin.id).token

        // Given - IN_REVIEW 상태 게시글 생성
        val post =
            createTestPost(
                title = "검토 중인 게시글",
                body = "현재 검토 중인 게시글입니다. 리뷰 단계에서도 삭제가 가능해야 합니다.",
                status = PostStatus.IN_REVIEW,
            )

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.delete("/api/v1/posts/{postId}", post.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent)

        // 삭제 확인
        val deletedPost = postRepository.findBy(post.id)
        assert(deletedPost?.status == PostStatus.DELETED)
    }

    private fun createTestPost(
        title: String,
        body: String,
        status: PostStatus,
        tags: Set<PostTag> = emptySet(),
    ): Post {
        val now = Instant.now()
        val post =
            Post.create(
                id = postIdGenerator.next(),
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

    private fun createAdminUser(): User {
        val now = Instant.now()
        val admin =
            User.create(
                id = UserId(1L),
                email = Email("admin@test.com"),
                encodedPassword = EncodedPassword("encodedPassword123"),
                nickname = Nickname("Admin"),
                status = UserStatus.ACTIVE,
                role = UserRole.ADMIN,
                createdAt = now,
                updatedAt = now,
            )
        return userRepository.save(admin)
    }

    private fun createTestUser(): User {
        val now = Instant.now()
        val user =
            User.create(
                id = UserId(2L),
                email = Email("user@test.com"),
                encodedPassword = EncodedPassword("encodedPassword456"),
                nickname = Nickname("TestUser"),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = now,
                updatedAt = now,
            )
        return userRepository.save(user)
    }

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
}
