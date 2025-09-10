package me.helloc.techwikiplus.post.dto.request

data class PostRequest(
    val title: String,
    val body: String,
    val tags: List<String>? = null,
)
