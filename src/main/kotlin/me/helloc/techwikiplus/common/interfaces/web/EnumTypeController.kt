package me.helloc.techwikiplus.common.interfaces.web

import me.helloc.techwikiplus.post.domain.model.review.ReviewCommentType
import me.helloc.techwikiplus.post.dto.response.ReviewCommentTypeResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/types")
class EnumTypeController {
    @GetMapping("/review-comments", produces = ["application/json"])
    fun getReviewCommentTypes(): ResponseEntity<List<ReviewCommentTypeResponse>> {
        val types =
            ReviewCommentType.entries.map { type ->
                ReviewCommentTypeResponse.from(
                    type = type.name,
                    description = type.description,
                )
            }
        return ResponseEntity.ok(types)
    }
}
