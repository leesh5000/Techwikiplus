package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.PostReviewService
import me.helloc.techwikiplus.post.interfaces.web.dto.PostReviewResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PostReviewsQueryController(
    private val postReviewService: PostReviewService,
) {
    @GetMapping("/api/v1/posts/{postId}/reviews")
    fun getPostReviews(
        @PathVariable postId: String,
    ): ResponseEntity<List<PostReviewResponse>> {
        val reviews = postReviewService.getReviewsByPostId(PostId.from(postId))
        return ResponseEntity.ok(reviews.map { PostReviewResponse.from(it) })
    }
}
