package me.helloc.techwikiplus.user.interfaces.web

import me.helloc.techwikiplus.user.domain.service.MyProfileService
import me.helloc.techwikiplus.user.dto.response.UserProfileResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MyProfileController(
    private val service: MyProfileService,
) {
    @GetMapping("/api/v1/users/me")
    fun getMyProfile(): ResponseEntity<UserProfileResponse> {
        val response = service.getMyProfile()
        return ResponseEntity.ok(response)
    }
}
