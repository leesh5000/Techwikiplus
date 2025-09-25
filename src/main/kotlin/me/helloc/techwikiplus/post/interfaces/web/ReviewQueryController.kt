package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.service.PostReviewService
import me.helloc.techwikiplus.post.dto.response.PostReviewResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ReviewQueryController(
    private val postReviewService: PostReviewService,
) {
    @GetMapping("/api/v1/reviews/{reviewId}")
    fun getReview(
        @PathVariable reviewId: String,
    ): ResponseEntity<PostReviewResponse> {
        val review = postReviewService.getById(PostReviewId.from(reviewId))
        return ResponseEntity.ok(PostReviewResponse.from(review))
    }
}
