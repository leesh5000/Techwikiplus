package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class DeletePostService(
    private val postReadService: PostReadService,
    private val postAuthorizationService: PostAuthorizationService,
    private val clockHolder: ClockHolder,
    private val repository: PostRepository,
) {
    fun deleteSoft(postId: PostId) {
        // 1. 권한 검증: 관리자 권한 확인
        postAuthorizationService.requireAdminRole()

        // 2. 기존 게시글 조회
        val post = postReadService.getBy(postId)

        // 3. 게시글 삭제 (Soft Delete)
        // Soft Delete는 태그 카운트를 감소시키지 않음 (복원 가능하므로)
        val now = clockHolder.now()
        val deletedPost = post.delete(now)
        return repository.save(deletedPost).let { deletedPost }
    }
}