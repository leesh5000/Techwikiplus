package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.common.infrastructure.security.context.SecurityContextService
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.service.RevisionVoteService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RevisionVoteController(
    private val revisionVoteService: RevisionVoteService,
    private val securityContextService: SecurityContextService,
) {
    @PostMapping("/api/v1/revisions/{revisionId}/votes")
    fun vote(
        @PathVariable revisionId: String,
    ): ResponseEntity<Void> {
        val voterId = securityContextService.getCurrentUserId()?.value

        revisionVoteService.vote(
            revisionId = PostRevisionId(revisionId.toLong()),
            voterId = voterId,
        )

        return ResponseEntity.noContent().build()
    }
}
