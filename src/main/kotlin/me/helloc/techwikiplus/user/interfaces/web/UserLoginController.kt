package me.helloc.techwikiplus.user.interfaces.web

import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.RawPassword
import me.helloc.techwikiplus.user.domain.service.UserLoginService
import me.helloc.techwikiplus.user.dto.request.UserLoginRequest
import me.helloc.techwikiplus.user.dto.response.UserLoginResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserLoginController(
    private val service: UserLoginService,
) {
    @PostMapping("/api/v1/users/login")
    fun login(
        @RequestBody request: UserLoginRequest,
    ): ResponseEntity<UserLoginResponse> {
        val response =
            service.login(
                email = Email(value = request.email),
                password = RawPassword(value = request.password),
            )
        return ResponseEntity.ok(response)
    }
}
