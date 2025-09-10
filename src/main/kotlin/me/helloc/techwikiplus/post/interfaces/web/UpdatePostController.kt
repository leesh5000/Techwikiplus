package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.UpdatePostService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class UpdatePostController(
    private val service: UpdatePostService,
) {
    @PutMapping("/api/v1/posts/{postId}", consumes = ["application/json"])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun update(
        @PathVariable postId: Long,
        @RequestBody request: Request,
    ) {
        val tagNames = request.tags?.map { TagName(it) } ?: emptyList()

        service.update(
            postId = PostId(postId),
            title = PostTitle(request.title),
            body = PostBody(request.body),
            tagNames = tagNames,
        )
    }

    data class Request(
        val title: String,
        val body: String,
        val tags: List<String>? = null,
    )
}
