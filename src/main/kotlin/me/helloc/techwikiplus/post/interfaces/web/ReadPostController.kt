package me.helloc.techwikiplus.post.interfaces.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ReadPostController {

    @GetMapping("/api/v1/posts/{postId}", produces = ["application/json"])
    fun read(@PathVariable postId: String) {

    }

    data class Response(
        val id: String,
        val title: String,
        val body: String,
        val createdAt: String,
        val modifiedAt: String,
        ) {

    }

}