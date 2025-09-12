package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.PostReadService
import me.helloc.techwikiplus.post.dto.response.PostScrollResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PostScrollQueryController(
    private val service: PostReadService,
) {
    @GetMapping("/api/v1/posts", produces = ["application/json"])
    fun execute(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<PostScrollResponse> {
        // cursor 문자열을 PostId로 변환
        val postCursor = cursor?.let { PostId(it.toLong()) }

        // UseCase 호출
        val response =
            service.getBy(
                cursor = postCursor,
                limit = limit,
            )
        return ResponseEntity.ok(response)
    }
}
