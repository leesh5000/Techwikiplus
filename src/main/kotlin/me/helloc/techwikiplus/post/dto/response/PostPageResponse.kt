package me.helloc.techwikiplus.post.dto.response

data class PostPageResponse(
    val posts: List<PostSummaryResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
)
