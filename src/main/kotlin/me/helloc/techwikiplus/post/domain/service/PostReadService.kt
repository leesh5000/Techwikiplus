package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import me.helloc.techwikiplus.post.dto.PostScrollResponse
import me.helloc.techwikiplus.post.dto.PostScrollResponse.PostSummaryResponse
import me.helloc.techwikiplus.post.dto.PostScrollResponse.PostSummaryResponse.TagResponse
import org.springframework.stereotype.Service

@Service
class PostReadService(
    private val repository: PostRepository,
) {
    fun getBy(postId: PostId): Post {
        return repository.findBy(postId)
            ?: throw PostDomainException(PostErrorCode.POST_NOT_FOUND, arrayOf(postId))
    }

    fun getBy(
        cursor: PostId?,
        limit: Int,
    ): PostScrollResponse {
        validateLimit(limit)

        // limit + 1개 조회하여 다음 페이지 존재 여부 확인
        val posts =
            repository.findAll(
                cursor = cursor,
                limit = limit + 1,
                excludeDeleted = true,
            )

        val hasNext = posts.size > limit
        val resultPosts = if (hasNext) posts.dropLast(1) else posts
        val nextCursor = if (hasNext) resultPosts.lastOrNull()?.id else null

        return PostScrollResponse(
            posts =
                resultPosts.map { post ->
                    PostSummaryResponse(
                        id = post.id.value.toString(),
                        title = post.title.value,
                        summary = extractSummary(post.body.value),
                        status = post.status.name,
                        tags =
                            post.tags.map { tag ->
                                TagResponse(
                                    name = tag.tagName.value,
                                    displayOrder = tag.displayOrder,
                                )
                            },
                        createdAt = post.createdAt.toString(),
                        updatedAt = post.updatedAt.toString(),
                    )
                },
            hasNext = hasNext,
            nextCursor = nextCursor?.value?.toString(),
        )
    }

    private fun validateLimit(limit: Int) {
        if (limit !in SCROLL_MIN_LIMIT..SCROLL_MAX_LIMIT) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.INVALID_PAGINATION_LIMIT,
                params = arrayOf(SCROLL_MIN_LIMIT, SCROLL_MAX_LIMIT),
            )
        }
    }

    fun extractSummary(body: String): String {
        val trimmedBody = body.trim()
        return if (trimmedBody.length <= SUMMARY_LENGTH) {
            trimmedBody
        } else {
            trimmedBody.take(SUMMARY_LENGTH) + "..."
        }
    }

    companion object {
        const val SCROLL_MIN_LIMIT = 1
        const val SCROLL_MAX_LIMIT = 100
        private const val SUMMARY_LENGTH = 200
    }
}
