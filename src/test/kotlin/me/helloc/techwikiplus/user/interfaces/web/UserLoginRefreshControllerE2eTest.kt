package me.helloc.techwikiplus.user.interfaces.web

import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema.Companion.schema
import me.helloc.techwikiplus.common.config.BaseE2eTest
import me.helloc.techwikiplus.common.config.annotations.E2eTest
import me.helloc.techwikiplus.common.config.documentation.withStandardErrorResponse
import me.helloc.techwikiplus.common.infrastructure.id.SnowflakeUserIdGenerator
import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.user.domain.model.Nickname
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole
import me.helloc.techwikiplus.user.domain.model.UserStatus
import me.helloc.techwikiplus.user.domain.service.port.TokenManager
import me.helloc.techwikiplus.user.domain.service.port.UserRepository
import me.helloc.techwikiplus.user.dto.request.UserLoginRefreshRequest
import me.helloc.techwikiplus.user.dto.response.UserLoginResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Instant

/**
 * UserLoginRefreshController 통합 테스트
 *
 * - 전체 애플리케이션 컨텍스트 로드
 * - TestContainers를 통한 실제 DB 연동
 * - 실제 토큰 갱신 로직 검증
 * - End-to-End 검증
 * - API 문서 자동 생성 (generateDocs = true)
 */
@E2eTest(generateDocs = true)
@TestPropertySource(
    properties = [
        "spring.application.name=techwikiplus-user",
        "spring.application.version=1.0.0-INTEGRATION",
        "api.documentation.enabled=true",
        "jwt.refresh-token-validity-in-seconds=1800", // 테스트용 30분 설정
    ],
)
class UserLoginRefreshControllerE2eTest : BaseE2eTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var snowflakeIdGenerator: SnowflakeUserIdGenerator

    @Autowired
    private lateinit var tokenManager: TokenManager

    // BCrypt로 암호화된 "Password123!" 문자열
    // DelegatingPasswordEncoder를 위해 {noop} 접두사 추가
    private val encodedPassword = "{noop}Password123!"

    @Test
    fun `POST login refresh - 유효한 리프레시 토큰으로 200 OK와 새로운 토큰 정보를 반환해야 한다`() {
        // Given
        val user = createActiveUser("refresh-test@example.com", "리프레시테스터")

        // 먼저 로그인하여 리프레시 토큰을 받아옴
        val loginTokens = loginUser(user)
        val refreshToken = loginTokens.refreshToken

        val request =
            UserLoginRefreshRequest(
                userId = user.id.value.toString(),
                refreshToken = refreshToken,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/login/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.accessToken").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.userId")
                    .value(user.id.toString()),
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.accessTokenExpiresAt").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.refreshTokenExpiresAt").exists())
            .andDo(
                documentWithResource(
                    "토큰 갱신 성공",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User")
                        .summary("토큰 갱신")
                        .description(
                            """
                            리프레시 토큰을 사용하여 새로운 액세스 토큰과 리프레시 토큰을 발급받습니다.

                            토큰 갱신이 성공하면 새로운 액세스 토큰과 리프레시 토큰이 모두 발급됩니다.

                            리프레시 토큰이 만료되거나 유효하지 않은 경우 다시 로그인해야 합니다.
                            """.trimIndent(),
                        )
                        .requestFields(
                            PayloadDocumentation.fieldWithPath("userId")
                                .type(JsonFieldType.STRING)
                                .description("사용자 고유 식별자"),
                            PayloadDocumentation.fieldWithPath("refreshToken")
                                .type(JsonFieldType.STRING)
                                .description("리프레시 토큰"),
                        )
                        .responseFields(
                            PayloadDocumentation.fieldWithPath("accessToken")
                                .type(JsonFieldType.STRING)
                                .description("새로운 액세스 토큰"),
                            PayloadDocumentation.fieldWithPath("refreshToken")
                                .type(JsonFieldType.STRING)
                                .description("새로운 리프레시 토큰"),
                            PayloadDocumentation.fieldWithPath("userId")
                                .type(JsonFieldType.STRING)
                                .description("사용자 고유 식별자"),
                            PayloadDocumentation.fieldWithPath("accessTokenExpiresAt")
                                .type(JsonFieldType.STRING)
                                .description("액세스 토큰 만료 시간 (ISO-8601 형식)"),
                            PayloadDocumentation.fieldWithPath("refreshTokenExpiresAt")
                                .type(JsonFieldType.STRING)
                                .description("리프레시 토큰 만료 시간 (ISO-8601 형식)"),
                        )
                        .requestSchema(
                            schema(
                                "${UserLoginRefreshRequest::class.simpleName}",
                            ),
                        )
                        .responseSchema(
                            schema(
                                "${UserLoginResponse::class.simpleName}",
                            ),
                        )
                        .build(),
                ),
            )
    }

    @Test
    fun `POST login refresh - 잘못된 형식의 리프레시 토큰으로 401 Unauthorized를 반환해야 한다`() {
        // Given
        val user = createActiveUser("invalid-format@example.com", "잘못된토큰테스터")

        val request =
            UserLoginRefreshRequest(
                userId = user.id.value.toString(),
                refreshToken = "invalid.token.format",
            )

        // When & Then
        // 잘못된 형식의 토큰은 MalformedJwtException을 발생시켜 INVALID_TOKEN으로 처리됨
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/login/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_TOKEN"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
            .andDo(
                documentWithResource(
                    "잘못된 형식의 리프레시 토큰으로 갱신",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User")
                        .summary("토큰 갱신 - 잘못된 형식의 토큰")
                        .description("유효하지 않은 형식의 리프레시 토큰으로 갱신 시도하는 경우 401 Unauthorized를 반환합니다.")
                        .requestSchema(
                            schema(
                                "${UserLoginRefreshRequest::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST login refresh - 다른 사용자의 리프레시 토큰으로 401 Unauthorized를 반환해야 한다`() {
        // Given
        val user1 = createActiveUser("user1-refresh@example.com", "사용자1")
        val user2 = createActiveUser("user2-refresh@example.com", "사용자2")

        // user2로 로그인하여 리프레시 토큰을 받아옴
        val loginTokens = loginUser(user2)

        // user1의 ID와 user2의 리프레시 토큰으로 요청
        val request =
            UserLoginRefreshRequest(
                userId = user1.id.value.toString(),
                refreshToken = loginTokens.refreshToken,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/login/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_TOKEN"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
            .andDo(
                documentWithResource(
                    "다른 사용자의 리프레시 토큰으로 갱신",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User")
                        .summary("토큰 갱신 - 다른 사용자의 토큰")
                        .description("다른 사용자의 리프레시 토큰으로 갱신 시도하는 경우 401 Unauthorized를 반환합니다.")
                        .requestSchema(
                            schema(
                                "${UserLoginRefreshRequest::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST login refresh - BANNED 상태 사용자로 403 Forbidden을 반환해야 한다`() {
        // Given
        val user = createActiveUser("banned-refresh@example.com", "차단된갱신자")

        // 먼저 유효한 토큰을 받아둠
        val loginTokens = loginUser(user)

        // BANNED 상태로 변경
        val bannedUser = user.copy(status = UserStatus.BANNED)
        userRepository.save(bannedUser)

        val request =
            UserLoginRefreshRequest(
                userId = user.id.value.toString(),
                refreshToken = loginTokens.refreshToken,
            )

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/login/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_BANNED"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.message").value("차단된 계정입니다. 관리자에게 문의해주세요"),
            )
            .andDo(
                documentWithResource(
                    "차단된 사용자 토큰 갱신",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User")
                        .summary("토큰 갱신 - 차단된 사용자")
                        .description("BANNED 상태의 사용자가 토큰 갱신을 시도하는 경우 403 Forbidden을 반환합니다.")
                        .requestSchema(
                            schema(
                                "${UserLoginRefreshRequest::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    @Test
    fun `POST login refresh - 존재하지 않는 사용자 ID로 404 Not Found를 반환해야 한다`() {
        // Given
        val request =
            UserLoginRefreshRequest(
                userId = "99999999999",
                refreshToken = "some.refresh.token",
            )

        // When & Then
        // 존재하지 않는 사용자 조회 시 404 반환
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/users/login/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_NOT_FOUND"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
            .andDo(
                documentWithResource(
                    "존재하지 않는 사용자 토큰 갱신",
                    ResourceSnippetParameters.Companion.builder()
                        .tag("User")
                        .summary("토큰 갱신 - 존재하지 않는 사용자")
                        .description("존재하지 않는 사용자 ID로 갱신을 시도하는 경우 404 Not Found를 반환합니다.")
                        .requestSchema(
                            schema(
                                "${UserLoginRefreshRequest::class.simpleName}",
                            ),
                        )
                        .withStandardErrorResponse()
                        .build(),
                ),
            )
    }

    private fun createActiveUser(
        email: String,
        nickname: String,
    ): User {
        return createUserWithStatus(email, nickname, UserStatus.ACTIVE)
    }

    private fun createUserWithStatus(
        email: String,
        nickname: String,
        status: UserStatus,
    ): User {
        val user =
            User.Companion.create(
                id = snowflakeIdGenerator.next(),
                email = Email(email),
                nickname = Nickname(nickname),
                encodedPassword = EncodedPassword(encodedPassword),
                status = status,
                role = UserRole.USER,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        return userRepository.save(user)
    }

    private fun loginUser(user: User): LoginTokens {
        val userId = UserId(user.id.value)
        val accessToken = tokenManager.generateAccessToken(userId)
        val refreshToken = tokenManager.generateRefreshToken(userId)

        return LoginTokens(
            accessToken = accessToken.token,
            refreshToken = refreshToken.token,
        )
    }

    private data class LoginTokens(
        val accessToken: String,
        val refreshToken: String,
    )
}
