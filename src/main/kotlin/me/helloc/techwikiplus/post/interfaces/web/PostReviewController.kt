package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.common.infrastructure.security.context.SecurityContextService
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.PostReviewService
import me.helloc.techwikiplus.post.dto.response.PostReviewResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class PostReviewController(
    private val postReviewService: PostReviewService,
    private val securityContextService: SecurityContextService,
) {
    @PostMapping(
        "/api/v1/posts/{postId}/reviews",
        produces = ["application/json"],
    )
    fun startReview(
        @PathVariable postId: String,
    ): ResponseEntity<PostReviewResponse> {
        // 현재 로그인한 사용자 ID 가져오기 (비로그인 시 null)
        val startedBy = securityContextService.getCurrentUserId()?.value

        val review =
            postReviewService.startReview(
                postId = PostId.from(postId),
                startedBy = startedBy,
            )

        val response =
            PostReviewResponse(
                reviewId = review.id.value.toString(),
                postId = review.postId.value.toString(),
                startedAt = review.startedAt,
                deadline = review.deadline,
                status = review.status.name,
            )

        val location =
            ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/../../../reviews/{id}")
                .buildAndExpand(review.id.value)
                .toUri()

        return ResponseEntity
            .created(location)
            .body(response)
    }
}
