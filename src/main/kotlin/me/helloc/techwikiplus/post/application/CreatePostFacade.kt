package me.helloc.techwikiplus.post.application

import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.PostAuthorizationService
import me.helloc.techwikiplus.post.domain.service.PostRegister
import me.helloc.techwikiplus.post.domain.service.TagPostCounter
import me.helloc.techwikiplus.post.domain.service.TagWriteService
import me.helloc.techwikiplus.post.interfaces.web.port.CreatePostUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class CreatePostFacade(
    private val postRegister: PostRegister,
    private val postAuthorizationService: PostAuthorizationService,
    private val tagWriteService: TagWriteService,
    private val tagPostCounter: TagPostCounter,
) : CreatePostUseCase {
    override fun handle(
        title: PostTitle,
        body: PostBody,
        tags: List<TagName>,
    ): PostId {
        postAuthorizationService.requireAdminRole()

        // 태그 생성 또는 조회
        val existingOrNewTags = tagWriteService.insert(tags)

        // 게시글 생성
        val post =
            postRegister.insert(
                title = title,
                body = body,
                tags = tags,
            )

        // 태그 사용 횟수 증가
        tagPostCounter.increment(existingOrNewTags)

        return post.id
    }
}
