package me.helloc.techwikiplus.post.domain.model.tag

class TagId(val value: Long) {
    init {
        require(value > 0) { "TagId must be positive" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TagId) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }
}
