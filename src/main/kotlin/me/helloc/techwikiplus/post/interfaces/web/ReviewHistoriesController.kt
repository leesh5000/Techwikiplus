package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.PostReviewService
import me.helloc.techwikiplus.post.dto.response.ReviewHistoryResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ReviewHistoriesController(
    private val postReviewService: PostReviewService,
) {
    @GetMapping("/api/v1/posts/{postId}/review-histories")
    fun getReviewHistories(
        @PathVariable postId: Long,
    ): ResponseEntity<List<ReviewHistoryResponse>> {
        val histories = postReviewService.getReviewHistories(PostId(postId))
        return ResponseEntity.ok(histories)
    }
}
