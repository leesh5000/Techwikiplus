package me.helloc.techwikiplus.post.domain.model.tag

data class PostTag(
    val tagName: TagName,
    val displayOrder: Int = 0,
) {
    init {
        require(displayOrder in 0..9) {
            "Display order must be between 0 and 9"
        }
    }

    override fun toString(): String {
        return "PostTag(tag=${tagName.value}, order=$displayOrder)"
    }
}
