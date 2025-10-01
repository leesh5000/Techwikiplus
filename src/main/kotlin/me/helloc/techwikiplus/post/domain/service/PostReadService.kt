package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import me.helloc.techwikiplus.post.dto.response.PostPageResponse
import me.helloc.techwikiplus.post.dto.response.PostResponse
import me.helloc.techwikiplus.post.dto.response.PostScrollResponse
import me.helloc.techwikiplus.post.dto.response.PostSummaryResponse
import org.springframework.stereotype.Service
import kotlin.math.ceil

@Service
class PostReadService(
    private val repository: PostRepository,
) {
    fun getPostScrollResponse(postId: PostId): Post {
        return repository.findBy(postId)
            ?: throw PostDomainException(PostErrorCode.POST_NOT_FOUND, arrayOf(postId))
    }

    fun getPostResponse(postId: PostId): PostResponse {
        val post = getPostScrollResponse(postId)
        return PostResponse.from(post)
    }

    fun getPostScrollResponse(
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
                    PostSummaryResponse.from(post)
                },
            hasNext = hasNext,
            nextCursor = nextCursor?.value?.toString(),
        )
    }

    fun getPostPageResponse(
        page: Int,
        size: Int,
    ): PostPageResponse {
        validatePageSize(size)
        validatePageNumber(page)

        val totalElements = repository.countAll(excludeDeleted = true)
        val totalPages = ceil(totalElements.toDouble() / size).toInt()

        val posts =
            repository.findAll(
                page = page,
                size = size,
                excludeDeleted = true,
            )

        return PostPageResponse(
            posts = posts.map { PostSummaryResponse.from(it) },
            totalElements = totalElements,
            totalPages = totalPages,
            currentPage = page,
            pageSize = size,
            hasNext = page < totalPages,
            hasPrevious = page > 1,
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

    private fun validatePageSize(size: Int) {
        if (size !in PAGE_MIN_SIZE..PAGE_MAX_SIZE) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.INVALID_PAGINATION_LIMIT,
                params = arrayOf(PAGE_MIN_SIZE, PAGE_MAX_SIZE),
            )
        }
    }

    private fun validatePageNumber(page: Int) {
        if (page < 1) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.INVALID_PAGE_NUMBER,
                params = arrayOf(page),
            )
        }
    }

    companion object {
        const val SCROLL_MIN_LIMIT = 1
        const val SCROLL_MAX_LIMIT = 100
        const val PAGE_MIN_SIZE = 1
        const val PAGE_MAX_SIZE = 100
        private const val SUMMARY_LENGTH = 200
    }
}
