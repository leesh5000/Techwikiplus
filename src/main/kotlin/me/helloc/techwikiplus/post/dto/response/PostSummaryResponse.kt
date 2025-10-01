package me.helloc.techwikiplus.post.dto.response

import me.helloc.techwikiplus.post.domain.model.post.Post

data class PostSummaryResponse(
    val id: String,
    val title: String,
    val summary: String,
    val status: String,
    val tags: List<TagResponse>,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun from(post: Post): PostSummaryResponse {
            return PostSummaryResponse(
                id = post.id.value.toString(),
                title = post.title.value,
                summary = post.body.summarize(),
                status = post.status.name,
                tags = post.tags.map { TagResponse.from(it) },
                createdAt = post.createdAt.toString(),
                updatedAt = post.updatedAt.toString(),
            )
        }
    }
}
