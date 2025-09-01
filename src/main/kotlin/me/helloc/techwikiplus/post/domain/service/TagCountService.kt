package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.TagJpaRepository
import me.helloc.techwikiplus.post.domain.model.tag.Tag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 태그 통계 관리 서비스
 *
 * 단일 책임 원칙에 따라 태그의 통계 관련 기능을 전담합니다.
 * TagService와 분리하여 각 서비스가 명확한 책임을 가지도록 설계했습니다.
 * - TagService: 태그 엔티티 생명주기 관리 (생성, 조회)
 * - TagCountService: 태그 통계 관리 (카운트 증가/감소)
 *
 * 이렇게 분리함으로써:
 * 1. 각 서비스의 책임이 명확해짐
 * 2. 테스트가 용이해짐
 * 3. 향후 통계 관련 기능 확장 시 이 서비스에 집중 가능
 */
@Service
@Transactional
class TagCountService(
    private val tagJpaRepository: TagJpaRepository,
) {
    /**
     * 태그들의 게시글 카운트를 증가시킵니다.
     *
     * 게시글이 생성되어 태그와 연결될 때 호출됩니다.
     * JPA Repository의 @Modifying 쿼리를 사용하여 벌크 업데이트를 수행합니다.
     */
    fun incrementPostCount(tags: Set<Tag>) {
        if (tags.isEmpty()) return

        tags.forEach { tag ->
            tagJpaRepository.incrementPostCount(tag.id.value)
        }
    }

    /**
     * 태그들의 게시글 카운트를 감소시킵니다.
     *
     * 게시글이 삭제되거나 태그가 제거될 때 호출됩니다.
     * 카운트가 0 이하로 내려가지 않도록 데이터베이스 레벨에서 보장합니다.
     */
    fun decrementPostCount(tags: Set<Tag>) {
        if (tags.isEmpty()) return

        tags.forEach { tag ->
            tagJpaRepository.decrementPostCount(tag.id.value)
        }
    }
}
