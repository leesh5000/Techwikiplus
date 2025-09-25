package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.PostHistoryService
import me.helloc.techwikiplus.post.dto.response.PostHistoryResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/posts")
class PostHistoryController(
    private val postHistoryService: PostHistoryService,
) {
    @GetMapping("/{postId}/histories")
    fun getHistories(
        @PathVariable postId: Long,
    ): ResponseEntity<List<PostHistoryResponse>> {
        val histories = postHistoryService.getHistories(PostId(postId))
        return ResponseEntity.ok(histories.map { PostHistoryResponse.from(it) })
    }
}
