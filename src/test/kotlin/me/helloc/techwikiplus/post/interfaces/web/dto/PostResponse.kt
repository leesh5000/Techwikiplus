package me.helloc.techwikiplus.post.interfaces.web.dto

data class PostResponse(
    val id: String,
    val title: String,
    val summary: String,
    val author: String,
    val createdAt: String,
    val updatedAt: String,
    val tags: List<String>,
)
