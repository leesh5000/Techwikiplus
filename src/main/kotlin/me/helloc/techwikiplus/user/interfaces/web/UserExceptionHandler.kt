package me.helloc.techwikiplus.user.interfaces.web

import me.helloc.techwikiplus.common.interfaces.web.ErrorResponse
import me.helloc.techwikiplus.common.interfaces.web.ErrorResponse.Companion.of
import me.helloc.techwikiplus.user.domain.exception.UserDomainException
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = ["me.helloc.techwikiplus.user"])
class UserExceptionHandler(
    private val userErrorCodeMapper: UserErrorCodeMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(UserDomainException::class)
    fun handleDomainException(e: UserDomainException): ResponseEntity<ErrorResponse> {
        val httpStatus = userErrorCodeMapper.mapToHttpStatus(e.userErrorCode)
        val message = userErrorCodeMapper.mapToMessage(e.userErrorCode, e.params)

        logger.warn("Domain exception occurred - ErrorCode: {}, Status: {}", e.userErrorCode, httpStatus)

        return ResponseEntity
            .status(httpStatus)
            .body(of(e.userErrorCode.name, message))
    }
}
