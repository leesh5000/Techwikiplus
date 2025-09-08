package me.helloc.techwikiplus.user.interfaces.web

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.Nickname
import me.helloc.techwikiplus.user.domain.model.RawPassword
import me.helloc.techwikiplus.user.domain.service.UserSignUpService
import me.helloc.techwikiplus.user.dto.request.UserSignUpRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserSignUpController(
    private val service: UserSignUpService,
) {
    @PostMapping("/api/v1/users/signup", consumes = ["application/json"])
    fun signup(
        @RequestBody request: UserSignUpRequest,
    ): ResponseEntity<Void> {
        service.signUp(
            email = Email(request.email),
            nickname = Nickname(request.nickname),
            password = RawPassword(request.password),
            passwordConfirm = RawPassword(request.confirmPassword),
        )

        val headers = HttpHeaders()
        headers.add(HttpHeaders.LOCATION, "/api/v1/users/verify")

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(headers)
            .build()
    }
}
