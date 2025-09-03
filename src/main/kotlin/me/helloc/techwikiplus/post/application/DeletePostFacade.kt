package me.helloc.techwikiplus.post.application

import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.PostAuthorizationService
import me.helloc.techwikiplus.post.domain.service.PostReadService
import me.helloc.techwikiplus.post.domain.service.PostWriteService
import me.helloc.techwikiplus.post.interfaces.web.port.DeletePostUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class DeletePostFacade(
    private val postReadService: PostReadService,
    private val postWriteService: PostWriteService,
    private val postAuthorizationService: PostAuthorizationService,
) : DeletePostUseCase {
    override fun handle(postId: PostId) {
        // 1. 권한 검증: 관리자 권한 확인
        postAuthorizationService.requireAdminRole()

        // 2. 기존 게시글 조회
        val post = postReadService.getBy(postId)

        // 3. 게시글 삭제 (Soft Delete)
        // Soft Delete는 태그 카운트를 감소시키지 않음 (복원 가능하므로)
        postWriteService.deleteSoft(post)
    }
}
