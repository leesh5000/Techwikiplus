package me.helloc.techwikiplus.common.infrastructure.persistence

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.PostJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.PostTagJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.TagJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostEntity
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostTagEntity
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.mapper.PostEntityMapper
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.tag.PostTag
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
@Transactional(readOnly = true)
class PostRepositoryImpl(
    private val jpaRepository: PostJpaRepository,
    private val postTagJpaRepository: PostTagJpaRepository,
    private val tagJpaRepository: TagJpaRepository,
    private val mapper: PostEntityMapper,
) : PostRepository {
    override fun findBy(id: PostId): Post? {
        // 기본 동작: DELETED 상태 제외
        return findBy(id, setOf(PostStatus.DELETED))
    }

    override fun findBy(
        id: PostId,
        excludedStatuses: Set<PostStatus>,
    ): Post? {
        val entity =
            when {
                excludedStatuses.isEmpty() -> {
                    // 모든 상태 포함
                    jpaRepository.findById(id.value).orElse(null)
                }
                excludedStatuses.size == 1 && excludedStatuses.first() == PostStatus.DELETED -> {
                    // DELETED만 제외 (최적화된 쿼리 사용)
                    jpaRepository.findByIdAndNotDeleted(id.value).orElse(null)
                }
                else -> {
                    // 여러 상태 제외 또는 DELETED가 아닌 다른 상태 제외 - 조회 후 필터링
                    jpaRepository.findById(id.value)
                        .orElse(null)
                        ?.takeIf { entity ->
                            excludedStatuses.none { it.name == entity.status }
                        }
                }
            }

        return entity?.let {
            val tags = loadTagsForPost(it.id)
            mapper.toDomain(it, tags)
        }
    }

    override fun existsBy(id: PostId): Boolean {
        // 기본 동작: DELETED 상태 제외
        return existsBy(id, setOf(PostStatus.DELETED))
    }

    override fun existsBy(
        id: PostId,
        excludedStatuses: Set<PostStatus>,
    ): Boolean {
        return when {
            excludedStatuses.isEmpty() -> {
                // 모든 상태 포함
                jpaRepository.existsById(id.value)
            }
            excludedStatuses.size == 1 && excludedStatuses.first() == PostStatus.DELETED -> {
                // DELETED만 제외 (최적화된 쿼리 사용)
                jpaRepository.existsByIdAndNotDeleted(id.value)
            }
            else -> {
                // 여러 상태 제외 또는 DELETED가 아닌 다른 상태 제외 - 조회 후 필터링
                jpaRepository.findById(id.value)
                    .orElse(null)
                    ?.let { entity ->
                        excludedStatuses.none { it.name == entity.status }
                    } ?: false
            }
        }
    }

    @Transactional
    override fun save(post: Post): Post {
        // 게시글 엔티티 저장
        val entity = mapper.toEntity(post)
        val savedEntity = jpaRepository.save<PostEntity>(entity)

        // 기존 태그 관계 삭제
        postTagJpaRepository.deleteByPostId(savedEntity.id)

        // 새 태그 관계 저장
        if (post.tags.isNotEmpty()) {
            savePostTags(savedEntity.id, post.tags)
        }

        // 저장된 태그 다시 조회하여 도메인 모델 반환
        val savedTags = loadTagsForPost(savedEntity.id)
        return mapper.toDomain(savedEntity, savedTags)
    }

    override fun findByTag(
        tagName: TagName,
        offset: Int,
        limit: Int,
    ): List<Post> {
        // TODO: 태그별 게시글 조회 구현
        // 현재는 빈 리스트 반환 (인프라 구현은 별도로 진행)
        return emptyList()
    }

    private fun loadTagsForPost(postId: Long): List<PostTag> {
        val postTagEntities = postTagJpaRepository.findByPostIdOrderByDisplayOrder(postId)
        if (postTagEntities.isEmpty()) return emptyList()

        val tagIds = postTagEntities.map { it.tagId }
        val tagEntities = tagJpaRepository.findAllById(tagIds)
        val tagMap = tagEntities.associateBy { it.id }

        return postTagEntities.mapNotNull { postTagEntity ->
            tagMap[postTagEntity.tagId]?.let { tagEntity ->
                PostTag(
                    tagName = TagName(tagEntity.name),
                    displayOrder = postTagEntity.displayOrder,
                )
            }
        }
    }

    private fun savePostTags(
        postId: Long,
        tags: List<PostTag>,
    ) {
        val now = Instant.now()

        // 태그명으로 태그 엔티티 조회
        val tagNames = tags.map { it.tagName.value }
        val tagEntities = tagJpaRepository.findByNameIn(tagNames)
        val tagMap = tagEntities.associateBy { it.name }

        // PostTag 엔티티 생성 및 저장
        val postTagEntities =
            tags.mapNotNull { postTag ->
                tagMap[postTag.tagName.value]?.let { tagEntity ->
                    PostTagEntity(
                        postId = postId,
                        tagId = tagEntity.id,
                        displayOrder = postTag.displayOrder,
                        createdAt = now,
                    )
                }
            }

        if (postTagEntities.isNotEmpty()) {
            postTagJpaRepository.saveAll(postTagEntities)
        }
    }
}
