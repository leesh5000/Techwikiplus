package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.service.PostReadService
import me.helloc.techwikiplus.post.dto.response.PostPageResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PostPageQueryController(
    private val service: PostReadService,
) {
    @GetMapping("/api/v1/posts/pages", produces = ["application/json"])
    fun execute(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PostPageResponse> {
        val response =
            service.getPostPageResponse(
                page = page,
                size = size,
            )
        return ResponseEntity.ok(response)
    }
}
