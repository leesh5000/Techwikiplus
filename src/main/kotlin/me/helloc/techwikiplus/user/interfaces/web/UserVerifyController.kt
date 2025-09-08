package me.helloc.techwikiplus.user.interfaces.web

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.RegistrationCode
import me.helloc.techwikiplus.user.domain.service.UserVerifyService
import me.helloc.techwikiplus.user.dto.request.UserVerifyRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserVerifyController(
    private val service: UserVerifyService,
) {
    @PostMapping("/api/v1/users/verify")
    fun verify(
        @RequestBody request: UserVerifyRequest,
    ): ResponseEntity<Void> {
        service.verifyEmail(
            email = Email(request.email),
            registrationCode = RegistrationCode(request.registrationCode),
        )

        val headers = HttpHeaders()
        headers.add(HttpHeaders.LOCATION, "/api/v1/users/login")

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .headers(headers)
            .build()
    }
}
