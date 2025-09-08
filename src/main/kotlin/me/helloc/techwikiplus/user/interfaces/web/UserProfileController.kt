package me.helloc.techwikiplus.user.interfaces.web

import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.service.UserProfileService
import me.helloc.techwikiplus.user.dto.response.UserProfileResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class UserProfileController(
    private val service: UserProfileService,
) {
    @GetMapping("/api/v1/users/{userId}", produces = ["application/json"])
    fun getUserProfile(
        @PathVariable("userId") userId: String,
    ): ResponseEntity<UserProfileResponse> {
        val response =
            service.getUserProfile(
                UserId.from(userId),
            )
        return ResponseEntity.ok(response)
    }
}
