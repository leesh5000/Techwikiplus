package me.helloc.techwikiplus.post.dto

data class PostScrollResponse(
    val posts: List<PostSummaryResponse>,
    val hasNext: Boolean,
    val nextCursor: String?,
) {
    data class PostSummaryResponse(
        val id: String,
        val title: String,
        val summary: String,
        val status: String,
        val tags: List<TagResponse>,
        val createdAt: String,
        val updatedAt: String,
    ) {
        data class TagResponse(
            val name: String,
            val displayOrder: Int,
        )
    }
}
