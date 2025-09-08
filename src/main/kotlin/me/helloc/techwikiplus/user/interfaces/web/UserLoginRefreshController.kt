package me.helloc.techwikiplus.user.interfaces.web

import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.service.UserLoginRefreshService
import me.helloc.techwikiplus.user.dto.request.UserLoginRefreshRequest
import me.helloc.techwikiplus.user.dto.response.UserLoginResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserLoginRefreshController(
    private val service: UserLoginRefreshService,
) {
    @PostMapping("/api/v1/users/login/refresh")
    fun refreshLogin(
        @RequestBody request: UserLoginRefreshRequest,
    ): ResponseEntity<UserLoginResponse> {
        val response: UserLoginResponse =
            service.refreshLogin(
                userId = UserId(value = request.userId.toLong()),
                refreshToken = request.refreshToken,
            )
        return ResponseEntity.ok(response)
    }
}
