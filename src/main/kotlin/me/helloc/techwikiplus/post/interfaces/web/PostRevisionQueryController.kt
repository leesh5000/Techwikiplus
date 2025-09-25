package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.service.PostRevisionService
import me.helloc.techwikiplus.post.dto.response.PostRevisionResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class PostRevisionQueryController(
    private val postRevisionService: PostRevisionService,
) {
    @GetMapping("/revisions/{revisionId}")
    fun getRevision(
        @PathVariable revisionId: Long,
    ): ResponseEntity<PostRevisionResponse> {
        val response = postRevisionService.getRevisionByRevisionId(PostRevisionId(revisionId))
        return ResponseEntity.ok(response)
    }

    @GetMapping("/reviews/{reviewId}/revisions")
    fun getRevisionsByReviewId(
        @PathVariable reviewId: Long,
    ): ResponseEntity<List<PostRevisionResponse>> {
        val responses = postRevisionService.getRevisionByReviewId(PostReviewId(reviewId))
        return ResponseEntity.ok(responses)
    }
}
