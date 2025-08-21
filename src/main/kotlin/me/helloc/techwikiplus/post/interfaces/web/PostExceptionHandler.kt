package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.common.interfaces.web.ErrorResponse
import me.helloc.techwikiplus.common.interfaces.web.ErrorResponse.Companion.of
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = ["me.helloc.techwikiplus.post"])
class PostExceptionHandler(
    private val postErrorCodeMapper: PostErrorCodeMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(PostDomainException::class)
    fun handleDomainException(e: PostDomainException): ResponseEntity<ErrorResponse> {
        val httpStatus = postErrorCodeMapper.mapToHttpStatus(e.postErrorCode)
        val message = postErrorCodeMapper.mapToMessage(e.postErrorCode, e.params)

        logger.warn("Domain exception occurred - ErrorCode: {}, Status: {}", e.postErrorCode, httpStatus)

        return ResponseEntity
            .status(httpStatus)
            .body(of(e.postErrorCode.name, message))
    }
}
