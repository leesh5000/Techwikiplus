package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.common.infrastructure.security.context.SecurityContextService
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.service.PostRevisionService
import me.helloc.techwikiplus.post.dto.request.PostRequest
import me.helloc.techwikiplus.post.dto.response.PostRevisionResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class PostRevisionController(
    private val postRevisionService: PostRevisionService,
    private val securityContextService: SecurityContextService,
) {
    @PostMapping(
        "/api/v1/reviews/{reviewId}/revisions",
        consumes = ["application/json"],
        produces = ["application/json"],
    )
    fun submitRevision(
        @PathVariable reviewId: String,
        @RequestBody request: PostRequest,
    ): ResponseEntity<PostRevisionResponse> {
        // 현재 로그인한 사용자 ID 가져오기 (비로그인 시 null)
        val authorId = securityContextService.getCurrentUserId()?.value

        val revision =
            postRevisionService.submitRevision(
                reviewId = PostReviewId(reviewId.toLong()),
                title = PostTitle(request.title),
                body = PostBody(request.body),
                authorId = authorId,
            )

        val location =
            ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/../../../revisions/{id}")
                .buildAndExpand(revision.id.value)
                .toUri()

        return ResponseEntity
            .created(location)
            .build()
    }
}
