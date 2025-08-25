package me.helloc.techwikiplus.post.application

import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.PostAuthorizationService
import me.helloc.techwikiplus.post.domain.service.PostWriteService
import me.helloc.techwikiplus.post.domain.service.TagCountService
import me.helloc.techwikiplus.post.domain.service.TagService
import me.helloc.techwikiplus.post.interfaces.web.port.CreatePostUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class CreatePostFacade(
    private val postWriteService: PostWriteService,
    private val postAuthorizationService: PostAuthorizationService,
    private val tagService: TagService,
    private val tagCountService: TagCountService,
) : CreatePostUseCase {
    override fun handle(
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
        val post = postWriteService.insert(title, body, tagNames)

        // 4. 태그 카운트 증가: 게시글과 연결된 태그들의 사용 횟수 증가
        // TagCountService를 별도로 사용하여 책임 분리
        tagCountService.incrementPostCount(tags)

        return post.id
    }
}
