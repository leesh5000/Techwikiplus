package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.PostTag
import me.helloc.techwikiplus.post.domain.model.tag.Tag
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class UpdatePostService(
    private val postReadService: PostReadService,
    private val postAuthorizationService: PostAuthorizationService,
    private val tagService: TagService,
    private val tagCountService: TagCountService,
    private val clockHolder: ClockHolder,
    private val repository: PostRepository,
) {
    fun update(
        postId: PostId,
        title: PostTitle,
        body: PostBody,
        tagNames: List<TagName>,
    ) {
        // 1. 권한 검증: 관리자 권한 확인
        postAuthorizationService.requireAdminRole()

        // 2. 기존 게시글 조회
        val post: Post = postReadService.getPostScrollResponse(postId)
        val beforeTagNames = post.tags.map { it.tagName }

        // 3. 태그 조회 (없으면 생성)
        val newTags = tagService.findOrCreateTags(tagNames)
        val beforeTags = tagService.findByNames(beforeTagNames)

        // 4. 게시글 수정
        update(
            post = post,
            title = title,
            body = body,
            tags = newTags,
        )

        // 5. 태그 카운트 조정
        // 제거된 태그들의 카운트 감소
        tagCountService.decrementPostCount(beforeTags - newTags)
        // 추가된 태그들의 카운트 증가
        tagCountService.incrementPostCount(newTags - beforeTags)
    }

    private fun update(
        post: Post,
        title: PostTitle,
        body: PostBody,
        tags: Set<Tag> = emptySet(),
    ): Post {
        val now = clockHolder.now()
        val postTags =
            tags.mapIndexed { index, tag ->
                PostTag(tag.name, index)
            }.toSet()
        val updatedPost =
            post.copy(
                title = title,
                body = body,
                tags = postTags,
                updatedAt = now,
            )
        return repository.save(updatedPost)
    }
}
