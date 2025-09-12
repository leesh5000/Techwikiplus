package me.helloc.techwikiplus.common.infrastructure.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.helloc.techwikiplus.common.interfaces.web.ErrorResponse
import me.helloc.techwikiplus.user.interfaces.web.UserErrorCodeMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
    private val userErrorCodeMapper: UserErrorCodeMapper,
) : AuthenticationEntryPoint {
    companion object {
        private val logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint::class.java)
        private const val DEFAULT_ERROR_MESSAGE = "인증이 필요합니다"

        private val ERROR_MESSAGE_MAP =
            mapOf(
                "Full authentication is required to access this resource" to "이 리소스에 접근하려면 로그인이 필요합니다",
                "Authentication failed" to "인증에 실패했습니다",
                "Bad credentials" to "아이디 또는 비밀번호가 올바르지 않습니다",
                "JWT expired" to "인증 토큰이 만료되었습니다",
                "Invalid JWT token" to "유효하지 않은 인증 토큰입니다",
                "JWT token is expired or invalid" to "인증 토큰이 만료되었거나 유효하지 않습니다",
                "Access is denied" to "접근이 거부되었습니다",
            )
    }

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException?,
    ) {
        logger.debug("Authentication failed: ${authException?.message}")

        val (httpStatus, errorCode, errorMessage) =
            when (authException) {
                is UserStatusAuthenticationException -> {
                    val status = userErrorCodeMapper.mapToHttpStatus(authException.errorCode)
                    val message = userErrorCodeMapper.mapToMessage(authException.errorCode, emptyArray())
                    Triple(status, authException.errorCode.name, message)
                }
                else -> {
                    val message =
                        authException?.message?.let {
                            ERROR_MESSAGE_MAP[it] ?: DEFAULT_ERROR_MESSAGE
                        } ?: DEFAULT_ERROR_MESSAGE

                    Triple(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        message,
                    )
                }
            }

        val errorResponse =
            ErrorResponse.of(
                code = errorCode,
                message = errorMessage,
            )

        response.status = httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
