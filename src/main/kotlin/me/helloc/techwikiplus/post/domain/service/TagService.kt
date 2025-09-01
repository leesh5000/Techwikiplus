package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.post.domain.model.tag.Tag
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.port.TagIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.TagRepository
import me.helloc.techwikiplus.user.domain.service.port.LockManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * 태그 엔티티의 생명주기 관리 서비스
 *
 * 단일 책임 원칙에 따라 태그의 생성과 조회를 전담합니다.
 * 태그 통계 관리는 TagCountService가 담당합니다.
 */
@Service
class TagService(
    private val tagRepository: TagRepository,
    private val tagIdGenerator: TagIdGenerator,
    private val clockHolder: ClockHolder,
    private val lockManager: LockManager,
) {
    /**
     * 태그명 리스트를 받아 태그를 조회하거나 생성합니다.
     *
     * findOrCreate 패턴을 사용하는 이유:
     * 1. 원자성 보장: 동시에 같은 태그를 생성하려는 요청 간 race condition 방지
     * 2. 비즈니스 요구사항: "태그 확보"라는 단일 목적 달성
     * 3. 트랜잭션 일관성: 하나의 트랜잭션 내에서 태그 존재 여부와 생성을 원자적으로 처리
     *
     * 동시성 제어 전략:
     * - 입력 순서대로 태그를 처리하여 ID 생성 순서 보장
     * - 각 태그별로 개별 락을 사용하여 동시성 제어
     * - 순차 처리로 데드락 방지 (동시에 여러 락을 잡지 않음)
     * - DB의 UNIQUE 제약조건과 함께 이중 안전장치 구성
     */
    fun findOrCreateTags(tagNames: List<TagName>): Set<Tag> {
        if (tagNames.isEmpty()) return emptySet()

        // 1. 중복 제거 (입력 순서 유지)
        val uniqueNames = tagNames.distinct()

        // 2. 먼저 존재하는 태그들을 한 번에 조회
        // 불필요한 락 획득을 최소화하기 위해 배치 조회
        val existingTags = tagRepository.findAllBy(uniqueNames)
        val existingTagMap = existingTags.associateBy { it.name }

        // 3. 입력 순서대로 태그 처리
        // 존재하는 태그는 재사용, 없는 태그는 생성
        val result =
            uniqueNames.map { tagName ->
                existingTagMap[tagName] ?: findOrCreateTag(tagName)
            }

        return result.toSet()
    }

    /**
     * 단일 태그를 조회하거나 생성합니다.
     *
     * 동시성 제어:
     * - Redis 분산 락을 사용하여 동일 태그명에 대한 동시 생성 방지
     * - Double-check 패턴으로 락 획득 후 재확인
     * - DataIntegrityViolationException 처리로 UNIQUE 제약조건 위반 대응
     */
    private fun findOrCreateTag(tagName: TagName): Tag {
        val lockKey = "tag:create:${tagName.value}"

        return lockManager.executeWithLock(
            key = lockKey,
            waitTime = Duration.ofSeconds(3),
            leaseTime = Duration.ofSeconds(10),
        ) {
            // Double-check: 락 획득 후 다시 확인
            // 다른 스레드가 이미 생성했을 수 있음
            tagRepository.findBy(tagName)?.let {
                return@executeWithLock it
            }

            try {
                // 새 태그 생성
                val now = clockHolder.now()
                val newTag =
                    Tag.create(
                        id = tagIdGenerator.next(),
                        name = tagName,
                        now = now,
                    )
                tagRepository.save(newTag)
            } catch (e: DataIntegrityViolationException) {
                // UNIQUE 제약조건 위반 시 재조회
                // 극히 드물지만 락 메커니즘을 우회한 동시 생성이 발생한 경우
                tagRepository.findBy(tagName)
                    ?: throw IllegalStateException("태그 생성 실패: ${tagName.value}", e)
            }
        }
    }

    /**
     * 태그명 리스트로 기존 태그들을 조회합니다.
     *
     * 존재하지 않는 태그는 결과에 포함되지 않습니다.
     */
    fun findByNames(tagNames: List<TagName>): Set<Tag> {
        if (tagNames.isEmpty()) return emptySet()
        val uniqueTagNames: List<TagName> = tagNames.distinct()
        return tagRepository.findAllBy(uniqueTagNames).toSet()
    }

    /**
     * 단일 태그명으로 태그를 조회합니다.
     */
    fun findByName(tagName: TagName): Tag? {
        return tagRepository.findBy(tagName)
    }
}
