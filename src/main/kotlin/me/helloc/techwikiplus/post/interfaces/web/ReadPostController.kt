package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.PostReadService
import me.helloc.techwikiplus.post.dto.response.PostResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ReadPostController(
    private val service: PostReadService,
) {
    @GetMapping("/api/v1/posts/{postId}", produces = ["application/json"])
    fun read(
        @PathVariable postId: String,
    ): ResponseEntity<PostResponse> {
        // PostId로 변환 (유효성 검증 포함)
        val id = PostId(postId.toLong())
        val response: PostResponse = service.getPostResponse(id)
        return ResponseEntity.ok(response)
    }
}
