package me.helloc.techwikiplus.post.dto.response

import me.helloc.techwikiplus.post.domain.model.tag.PostTag

data class TagResponse(
    val name: String,
    val displayOrder: Int,
) {
    companion object {
        fun from(tag: PostTag): TagResponse {
            return TagResponse(
                name = tag.tagName.value,
                displayOrder = tag.displayOrder,
            )
        }
    }
}
