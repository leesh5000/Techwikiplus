package me.helloc.techwikiplus.post.dto

data class PostRequest(
    val title: String,
    val body: String,
    val tags: List<String>? = null,
)
