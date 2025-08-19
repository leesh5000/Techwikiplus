package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.model.PostBody
import me.helloc.techwikiplus.post.domain.model.PostTitle
import me.helloc.techwikiplus.post.interfaces.web.port.CreatePostUseCase
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CreatePostController(
    private val useCase: CreatePostUseCase,
) {
    @PostMapping("/api/v1/posts", consumes = ["application/json"])
    fun create(
        @RequestBody request: Request,
    ): ResponseEntity<Void> {
        val postId =
            useCase.handle(
                title = PostTitle(request.title),
                body = PostBody(request.body),
            )

        val headers = HttpHeaders()
        headers.add(HttpHeaders.LOCATION, "/api/v1/posts/${postId.value}")

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .headers(headers)
            .build()
    }

    data class Request(
        val title: String,
        val body: String,
    )
}
