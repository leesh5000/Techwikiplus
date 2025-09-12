package me.helloc.techwikiplus.user.interfaces.web

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.service.UserVerifyResendService
import me.helloc.techwikiplus.user.dto.request.UserVerifyResendRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserVerifyResendController(
    private val service: UserVerifyResendService,
) {
    @PostMapping("/api/v1/users/verify/resend")
    fun resend(
        @RequestBody request: UserVerifyResendRequest,
    ): ResponseEntity<Void> {
        service.verifyResend(
            email = Email(request.email),
        )

        val headers = HttpHeaders()
        headers.add(HttpHeaders.LOCATION, "/api/v1/users/verify")

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(headers)
            .build()
    }
}
