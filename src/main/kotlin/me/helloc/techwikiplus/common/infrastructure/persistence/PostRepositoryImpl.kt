package me.helloc.techwikiplus.common.infrastructure.persistence

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.PostJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostEntity
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.mapper.PostEntityMapper
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class PostRepositoryImpl(
    private val jpaRepository: PostJpaRepository,
    private val mapper: PostEntityMapper,
) : PostRepository {
    override fun findBy(id: PostId): Post? {
        return jpaRepository.findById(id.value)
            .orElse(null)
            ?.let {
                mapper.toDomain(it)
            }
    }

    override fun existsBy(id: PostId): Boolean {
        return jpaRepository.existsById(id.value)
    }

    @Transactional
    override fun save(post: Post): Post {
        val entity = mapper.toEntity(post)
        val savedEntity = jpaRepository.save<PostEntity>(entity)
        return mapper.toDomain(savedEntity)
    }

    override fun findByTag(tagName: TagName, offset: Int, limit: Int): List<Post> {
        // TODO: 태그별 게시글 조회 구현
        // 현재는 빈 리스트 반환 (인프라 구현은 별도로 진행)
        return emptyList()
    }
}
