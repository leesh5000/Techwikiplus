package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.service.CreatePostService
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.dto.PostRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CreatePostController(
    private val service: CreatePostService,
) {
    @PostMapping("/api/v1/posts", consumes = ["application/json"])
    fun create(
        @RequestBody request: PostRequest,
    ): ResponseEntity<Void> {
        val tagNames = request.tags?.map { TagName(it) } ?: emptyList()

        val postId =
            service.createPost(
                title = PostTitle(request.title),
                body = PostBody(request.body),
                tagNames = tagNames,
            )

        val headers = HttpHeaders()
        headers.add(HttpHeaders.LOCATION, "/api/v1/posts/${postId.value}")

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .headers(headers)
            .build()
    }
}
