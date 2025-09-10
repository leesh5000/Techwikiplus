package me.helloc.techwikiplus.post.dto.response

import me.helloc.techwikiplus.post.domain.model.post.Post

data class PostResponse(
    val id: String,
    val title: String,
    val body: String,
    val status: String,
    val viewCount: Int = 0,
    val tags: List<TagResponse>,
    val createdAt: String,
    val modifiedAt: String,
) {
    companion object {
        fun from(post: Post): PostResponse {
            return PostResponse(
                id = post.id.value.toString(),
                title = post.title.value,
                body = post.body.value,
                status = post.status.name,
                tags =
                    post.tags.map { tag ->
                        TagResponse.from(tag)
                    },
                createdAt = post.createdAt.toString(),
                modifiedAt = post.updatedAt.toString(),
            )
        }
    }
}
