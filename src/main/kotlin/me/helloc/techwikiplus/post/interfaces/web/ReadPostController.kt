package me.helloc.techwikiplus.post.interfaces.web

import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.interfaces.web.port.ReadPostUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ReadPostController(
    private val useCase: ReadPostUseCase,
) {
    @GetMapping("/api/v1/posts/{postId}", produces = ["application/json"])
    fun read(
        @PathVariable postId: String,
    ): ResponseEntity<Response> {
        // PostId로 변환 (유효성 검증 포함)
        val id = PostId(postId.toLong())

        // UseCase를 통해 게시글 조회
        val post = useCase.handle(id)

        // Domain 객체를 Response DTO로 변환
        val response =
            Response(
                id = post.id.value.toString(),
                title = post.title.value,
                body = post.body.value,
                status = post.status.name,
                tags =
                    post.tags.map {
                        TagResponse(
                            name = it.tagName.value,
                            displayOrder = it.displayOrder,
                        )
                    },
                createdAt = post.createdAt.toString(),
                modifiedAt = post.updatedAt.toString(),
            )

        return ResponseEntity.ok(response)
    }

    data class Response(
        val id: String,
        val title: String,
        val body: String,
        val status: String,
        val tags: List<TagResponse>,
        val createdAt: String,
        val modifiedAt: String,
    )

    data class TagResponse(
        val name: String,
        val displayOrder: Int,
    )
}
