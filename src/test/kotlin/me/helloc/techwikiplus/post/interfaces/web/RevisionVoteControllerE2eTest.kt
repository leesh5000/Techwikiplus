package me.helloc.techwikiplus.post.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.common.config.BaseE2eTest
import me.helloc.techwikiplus.common.config.annotations.E2eTest
import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.RevisionVoteJpaRepository
import me.helloc.techwikiplus.common.infrastructure.security.jwt.JwtTokenManager
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.review.PostReview
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostReviewStatus
import me.helloc.techwikiplus.post.domain.model.review.PostRevision
import me.helloc.techwikiplus.post.domain.model.review.RevisionVote
import me.helloc.techwikiplus.post.domain.service.port.PostReviewIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostReviewRepository
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionRepository
import me.helloc.techwikiplus.post.domain.service.port.RevisionVoteRepository
import me.helloc.techwikiplus.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.user.domain.model.Nickname
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.model.UserRole
import me.helloc.techwikiplus.user.domain.model.UserStatus
import me.helloc.techwikiplus.user.domain.service.port.UserIdGenerator
import me.helloc.techwikiplus.user.domain.service.port.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@E2eTest
class RevisionVoteControllerE2eTest : BaseE2eTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userIdGenerator: UserIdGenerator

    @Autowired
    private lateinit var postReviewRepository: PostReviewRepository

    @Autowired
    private lateinit var postReviewIdGenerator: PostReviewIdGenerator

    @Autowired
    private lateinit var postRevisionRepository: PostRevisionRepository

    @Autowired
    private lateinit var postRevisionIdGenerator: PostRevisionIdGenerator

    @Autowired
    private lateinit var revisionVoteRepository: RevisionVoteRepository

    @Autowired
    private lateinit var revisionVoteJpaRepository: RevisionVoteJpaRepository

    @Autowired
    private lateinit var jwtTokenManager: JwtTokenManager

    @Autowired
    private lateinit var clockHolder: ClockHolder

    private lateinit var testUser: User
    private lateinit var anotherUser: User
    private lateinit var testReview: PostReview
    private lateinit var testRevision: PostRevision
    private lateinit var validToken: String
    private lateinit var anotherUserToken: String

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = createTestUser("test@example.com", "testuser")
        anotherUser = createTestUser("another@example.com", "anotheruser")

        // JWT 토큰 생성
        validToken = jwtTokenManager.generateAccessToken(testUser.id).token
        anotherUserToken = jwtTokenManager.generateAccessToken(anotherUser.id).token

        // 테스트 리뷰와 리비전 생성
        testReview = createTestReview()
        testRevision = createTestRevision(testReview.id)
    }

    @Test
    fun `인증된 사용자가 리비전에 투표할 수 있다`() {
        // when & then
        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", testRevision.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken"),
        )
            .andExpect(status().isNoContent)
            .andDo(
                documentWithResource(
                    identifier = "revision-vote-create-success",
                    resourceParameters =
                        ResourceSnippetParameters.builder()
                            .tag("Revision Vote")
                            .summary("리비전 투표")
                            .description("특정 리비전에 투표합니다. 사용자당 하나의 투표만 가능합니다.")
                            .requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                    .description("JWT 인증 토큰 (Bearer 타입)"),
                            )
                            .pathParameters(
                                parameterWithName("revisionId")
                                    .description("투표할 리비전 ID"),
                            )
                            .responseSchema(Schema.schema("EmptyResponse"))
                            .build(),
                ),
            )

        // 투표가 저장되었는지 확인
        val voteExists = revisionVoteRepository.existsByRevisionIdAndVoterId(testRevision.id, testUser.id.value)
        voteExists shouldBe true

        // 리비전의 투표 수가 증가했는지 확인
        val updatedRevision = postRevisionRepository.findById(testRevision.id)
        updatedRevision?.voteCount shouldBe 1
    }

    @Test
    fun `같은 사용자가 동일한 리비전에 중복 투표하면 409 에러가 발생한다`() {
        // given
        val vote =
            RevisionVote(
                id = 0L,
                revisionId = testRevision.id,
                voterId = testUser.id.value,
                votedAt = clockHolder.now(),
            )
        revisionVoteRepository.save(vote)

        // when & then
        val result =
            mockMvc.perform(
                post("/api/v1/revisions/{revisionId}/votes", testRevision.id.value)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken"),
            )
                .andExpect(status().isConflict)
                .andDo { println("Response: ${it.response.contentAsString}") }
                .andReturn()

        // 응답 확인 후 errorCode 검증
        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", testRevision.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken"),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("ALREADY_VOTED"))
            .andDo(
                documentWithResource(
                    identifier = "revision-vote-create-duplicate",
                    resourceParameters =
                        ResourceSnippetParameters.builder()
                            .tag("Revision Vote")
                            .summary("리비전 중복 투표 실패")
                            .description("이미 투표한 리비전에 다시 투표하려고 하면 409 Conflict 에러가 발생합니다.")
                            .requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                    .description("JWT 인증 토큰 (Bearer 타입)"),
                            )
                            .pathParameters(
                                parameterWithName("revisionId")
                                    .description("투표할 리비전 ID"),
                            )
                            .responseSchema(Schema.schema("ErrorResponse"))
                            .build(),
                ),
            )
    }

    @Test
    fun `존재하지 않는 리비전에 투표하면 404 에러가 발생한다`() {
        // given
        val nonExistentRevisionId = 999999L

        // when & then
        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", nonExistentRevisionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("REVISION_NOT_FOUND"))
            .andDo(
                documentWithResource(
                    identifier = "revision-vote-create-not-found",
                    resourceParameters =
                        ResourceSnippetParameters.builder()
                            .tag("Revision Vote")
                            .summary("존재하지 않는 리비전 투표 실패")
                            .description("존재하지 않는 리비전에 투표하려고 하면 404 Not Found 에러가 발생합니다.")
                            .requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION)
                                    .description("JWT 인증 토큰 (Bearer 타입)"),
                            )
                            .pathParameters(
                                parameterWithName("revisionId")
                                    .description("투표할 리비전 ID"),
                            )
                            .responseSchema(Schema.schema("ErrorResponse"))
                            .build(),
                ),
            )
    }

    @Test
    fun `인증되지 않은 사용자가 투표하면 401 에러가 발생한다`() {
        // when & then
        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", testRevision.id.value),
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                documentWithResource(
                    identifier = "revision-vote-create-unauthorized",
                    resourceParameters =
                        ResourceSnippetParameters.builder()
                            .tag("Revision Vote")
                            .summary("비로그인 사용자 투표 시도")
                            .description("비로그인 사용자가 투표를 시도하면 401 Unauthorized 에러가 발생합니다.")
                            .pathParameters(
                                parameterWithName("revisionId")
                                    .description("투표할 리비전 ID"),
                            )
                            .responseSchema(Schema.schema("ErrorResponse"))
                            .build(),
                ),
            )
    }

    @Test
    fun `여러 사용자가 동일한 리비전에 투표할 수 있다`() {
        // given
        val user3 = createTestUser("user3@example.com", "user3")
        val user3Token = jwtTokenManager.generateAccessToken(user3.id).token

        // when
        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", testRevision.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken"),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", testRevision.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $anotherUserToken"),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", testRevision.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $user3Token"),
        )
            .andExpect(status().isNoContent)

        // then
        val voteEntities = revisionVoteJpaRepository.findAll()
        val revisionVotes = voteEntities.filter { it.revisionId == testRevision.id.value }
        revisionVotes.size shouldBe 3

        val voterIds = revisionVotes.map { it.voterId }.toSet()
        voterIds shouldBe setOf(testUser.id.value, anotherUser.id.value, user3.id.value)

        // 리비전의 투표 수가 3으로 증가했는지 확인
        val updatedRevision = postRevisionRepository.findById(testRevision.id)
        updatedRevision?.voteCount shouldBe 3
    }

    @Test
    fun `한 사용자가 여러 리비전에 각각 투표할 수 있다`() {
        // given
        val anotherRevision =
            createTestRevision(testReview.id, "Another revision content with more than 30 characters for testing multiple votes.")

        // when
        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", testRevision.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken"),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", anotherRevision.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken"),
        )
            .andExpect(status().isNoContent)

        // then
        val voteExists1 = revisionVoteRepository.existsByRevisionIdAndVoterId(testRevision.id, testUser.id.value)
        voteExists1 shouldBe true

        val voteExists2 = revisionVoteRepository.existsByRevisionIdAndVoterId(anotherRevision.id, testUser.id.value)
        voteExists2 shouldBe true
    }

    @Test
    fun `만료된 토큰으로 투표하면 401 에러가 발생한다`() {
        // given
        val expiredToken = "expired.token.here"

        // when & then
        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", testRevision.id.value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $expiredToken"),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `잘못된 형식의 리비전 ID로 투표하면 400 에러가 발생한다`() {
        // when & then
        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", "invalid-id")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `음수 리비전 ID로 투표하면 400 에러가 발생한다`() {
        // when & then
        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", "-1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `0인 리비전 ID로 투표하면 400 에러가 발생한다`() {
        // when & then
        mockMvc.perform(
            post("/api/v1/revisions/{revisionId}/votes", "0")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken"),
        )
            .andExpect(status().isBadRequest)
    }

    private fun createTestUser(
        email: String,
        nickname: String,
    ): User {
        val user =
            User(
                id = userIdGenerator.next(),
                email = me.helloc.techwikiplus.user.domain.model.Email(email),
                nickname = Nickname(nickname),
                encodedPassword = EncodedPassword("encodedPassword123"),
                status = UserStatus.ACTIVE,
                role = UserRole.USER,
                createdAt = clockHolder.now(),
                updatedAt = clockHolder.now(),
            )
        return userRepository.save(user)
    }

    private fun createTestReview(): PostReview {
        val review =
            PostReview(
                id = postReviewIdGenerator.generate(),
                postId = PostId(1L),
                startedAt = clockHolder.now(),
                // 72시간 후
                deadline = clockHolder.now().plusSeconds(259200),
                status = PostReviewStatus.IN_REVIEW,
                winningRevisionId = null,
                startedBy = testUser.id.value,
            )
        return postReviewRepository.save(review)
    }

    private fun createTestRevision(
        reviewId: PostReviewId,
        content: String = "This is a test revision content with at least 30 characters to satisfy validation requirements.",
    ): PostRevision {
        val revision =
            PostRevision(
                id = postRevisionIdGenerator.generate(),
                reviewId = reviewId,
                authorId = anotherUser.id.value,
                title = PostTitle("Test Title"),
                body = PostBody(content),
                submittedAt = clockHolder.now(),
                voteCount = 0,
            )
        return postRevisionRepository.save(revision)
    }
}
