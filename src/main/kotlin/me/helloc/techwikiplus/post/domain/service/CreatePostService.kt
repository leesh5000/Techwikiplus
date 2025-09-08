package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.PostTag
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.port.PostIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class CreatePostService(
    private val postAuthorizationService: PostAuthorizationService,
    private val tagService: TagService,
    private val tagCountService: TagCountService,
    private val clockHolder: ClockHolder,
    private val postIdGenerator: PostIdGenerator,
    private val repository: PostRepository,
) {
    fun createPost(
        title: PostTitle,
        body: PostBody,
        tagNames: List<TagName>,
    ): PostId {
        // 1. 정책: 관리자만 게시글을 등록할 수 있습니다.
        postAuthorizationService.requireAdminRole()

        // 2. 태그 처리: 존재하지 않는 태그는 새로 생성, 기존 태그는 재사용
        // 이를 통해 태그의 일관성을 보장하고 중복 생성을 방지
        val tags = tagService.findOrCreateTags(tagNames)

        // 3. 게시글 생성
        // 태그명 리스트를 전달하여 게시글과 태그를 연결
        val post = insert(title, body, tagNames)

        // 4. 태그 카운트 증가: 게시글과 연결된 태그들의 사용 횟수 증가
        tagCountService.incrementPostCount(tags)
        return post.id
    }

    fun insert(
        title: PostTitle,
        body: PostBody,
        tagNames: List<TagName> = emptyList(),
    ): Post {
        val now = clockHolder.now()
        val postTags =
            tagNames.mapIndexed { index, tagName ->
                PostTag(tagName, index)
            }.toSet()
        val post =
            Post.create(
                id = postIdGenerator.next(),
                title = title,
                body = body,
                postTags = postTags,
                createdAt = now,
                updatedAt = now,
            )

        return repository.save(post)
    }
}