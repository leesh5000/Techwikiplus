package me.helloc.techwikiplus.common.infrastructure.persistence

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.PostJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.PostTagJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.TagJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostEntity
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostTagEntity
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.mapper.PostEntityMapper
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId
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
        return jpaRepository.findById(id.value)
            .map { mapper.toDomain(it, loadTagsForPost(it.id)) }
            .orElse(null)
    }

    override fun existsBy(id: PostId): Boolean {
        // 기본 동작: DELETED 상태 제외
        return jpaRepository.existsById(id.value)
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
        tags: Set<PostTag>,
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
