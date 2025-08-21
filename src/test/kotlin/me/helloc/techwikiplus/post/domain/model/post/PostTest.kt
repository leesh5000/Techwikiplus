package me.helloc.techwikiplus.post.domain.model.post

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import java.time.Instant

class PostTest : DescribeSpec({

    describe("Post 생성") {

        context("유효한 데이터로 생성") {
            it("정상적으로 생성된다") {
                val now = Instant.now()
                val post =
                    Post.create(
                        id = PostId(1L),
                        title = PostTitle("테스트 제목"),
                        body = PostBody("테스트 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."),
                        createdAt = now,
                    )

                post.id shouldBe PostId(1L)
                post.title.value shouldBe "테스트 제목"
                post.body.value shouldBe "테스트 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."
                post.status shouldBe PostStatus.DRAFT
                post.createdAt shouldBe now
                post.updatedAt shouldBe now
            }

            it("상태를 지정하여 생성할 수 있다") {
                val now = Instant.now()
                val post =
                    Post.create(
                        id = PostId(1L),
                        title = PostTitle("테스트 제목"),
                        body = PostBody("테스트 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."),
                        status = PostStatus.IN_REVIEW,
                        createdAt = now,
                    )

                post.status shouldBe PostStatus.IN_REVIEW
            }
        }
    }

    describe("Post 상태 전이") {

        val now = Instant.now()
        val post =
            Post.create(
                id = PostId(1L),
                title = PostTitle("테스트 제목"),
                body = PostBody("테스트 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."),
                createdAt = now,
            )

        context("submitForReview") {
            it("DRAFT 상태에서 IN_REVIEW로 변경된다") {
                val updatedTime = now.plusSeconds(60)
                val reviewPost = post.submitForReview(updatedTime)

                reviewPost.status shouldBe PostStatus.IN_REVIEW
                reviewPost.updatedAt shouldBe updatedTime
            }

            it("DRAFT가 아닌 상태에서는 예외가 발생한다") {
                val inReviewPost = post.copy(status = PostStatus.IN_REVIEW)
                val exception =
                    shouldThrow<PostDomainException> {
                        inReviewPost.submitForReview(now.plusSeconds(60))
                    }
                exception.postErrorCode shouldBe PostErrorCode.INVALID_POST_STATE
            }
        }

        context("markAsReviewed") {
            it("IN_REVIEW 상태에서 REVIEWED로 변경된다") {
                val inReviewPost = post.copy(status = PostStatus.IN_REVIEW)
                val updatedTime = now.plusSeconds(60)
                val reviewedPost = inReviewPost.markAsReviewed(updatedTime)

                reviewedPost.status shouldBe PostStatus.REVIEWED
                reviewedPost.updatedAt shouldBe updatedTime
            }

            it("IN_REVIEW가 아닌 상태에서는 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        post.markAsReviewed(now.plusSeconds(60))
                    }
                exception.postErrorCode shouldBe PostErrorCode.INVALID_POST_STATE
            }
        }

        context("backToDraft") {
            it("어떤 상태에서든 DRAFT로 변경할 수 있다") {
                val reviewedPost = post.copy(status = PostStatus.REVIEWED)
                val updatedTime = now.plusSeconds(60)
                val draftPost = reviewedPost.backToDraft(updatedTime)

                draftPost.status shouldBe PostStatus.DRAFT
                draftPost.updatedAt shouldBe updatedTime
            }

            it("이미 DRAFT 상태면 그대로 반환한다") {
                val draftPost = post.backToDraft(now.plusSeconds(60))
                draftPost shouldBe post
            }
        }
    }

    describe("Post 내용 수정") {

        val now = Instant.now()
        val post =
            Post.create(
                id = PostId(1L),
                title = PostTitle("원본 제목"),
                body = PostBody("원본 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."),
                createdAt = now,
            )

        context("updateTitle") {
            it("DRAFT 상태에서 제목을 수정할 수 있다") {
                val newTitle = PostTitle("수정된 제목")
                val updatedTime = now.plusSeconds(60)
                val updatedPost = post.updateTitle(newTitle, updatedTime)

                updatedPost.title.value shouldBe "수정된 제목"
                updatedPost.updatedAt shouldBe updatedTime
            }

            it("IN_REVIEW 상태에서도 제목을 수정할 수 있다") {
                val inReviewPost = post.copy(status = PostStatus.IN_REVIEW)
                val newTitle = PostTitle("수정된 제목")
                val updatedPost = inReviewPost.updateTitle(newTitle, now.plusSeconds(60))

                updatedPost.title.value shouldBe "수정된 제목"
            }

            it("REVIEWED 상태에서는 제목을 수정할 수 없다") {
                val reviewedPost = post.copy(status = PostStatus.REVIEWED)
                val exception =
                    shouldThrow<PostDomainException> {
                        reviewedPost.updateTitle(PostTitle("새 제목"), now.plusSeconds(60))
                    }
                exception.postErrorCode shouldBe PostErrorCode.INVALID_POST_STATE
            }
        }

        context("updateBody") {
            it("DRAFT 상태에서 내용을 수정할 수 있다") {
                val newBody = PostBody("수정된 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 수정된 테스트 게시글입니다.")
                val updatedTime = now.plusSeconds(60)
                val updatedPost = post.updateBody(newBody, updatedTime)

                updatedPost.body.value shouldBe "수정된 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 수정된 테스트 게시글입니다."
                updatedPost.updatedAt shouldBe updatedTime
            }

            it("IN_REVIEW 상태에서도 내용을 수정할 수 있다") {
                val inReviewPost = post.copy(status = PostStatus.IN_REVIEW)
                val newBody = PostBody("수정된 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 수정된 테스트 게시글입니다.")
                val updatedPost = inReviewPost.updateBody(newBody, now.plusSeconds(60))

                updatedPost.body.value shouldBe "수정된 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 수정된 테스트 게시글입니다."
            }

            it("REVIEWED 상태에서는 내용을 수정할 수 없다") {
                val reviewedPost = post.copy(status = PostStatus.REVIEWED)
                val exception =
                    shouldThrow<PostDomainException> {
                        reviewedPost.updateBody(
                            PostBody("새 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."),
                            now.plusSeconds(60),
                        )
                    }
                exception.postErrorCode shouldBe PostErrorCode.INVALID_POST_STATE
            }
        }
    }

    describe("Post 상태 확인") {

        val now = Instant.now()

        it("isDraft는 DRAFT 상태일 때 true를 반환한다") {
            val draftPost =
                Post.create(
                    id = PostId(1L),
                    title = PostTitle("테스트 제목"),
                    body = PostBody("테스트 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."),
                    status = PostStatus.DRAFT,
                    createdAt = now,
                )

            draftPost.isDraft() shouldBe true
            draftPost.isInReview() shouldBe false
            draftPost.isReviewed() shouldBe false
        }

        it("isInReview는 IN_REVIEW 상태일 때 true를 반환한다") {
            val inReviewPost =
                Post.create(
                    id = PostId(1L),
                    title = PostTitle("테스트 제목"),
                    body = PostBody("테스트 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."),
                    status = PostStatus.IN_REVIEW,
                    createdAt = now,
                )

            inReviewPost.isDraft() shouldBe false
            inReviewPost.isInReview() shouldBe true
            inReviewPost.isReviewed() shouldBe false
        }

        it("isReviewed는 REVIEWED 상태일 때 true를 반환한다") {
            val reviewedPost =
                Post.create(
                    id = PostId(1L),
                    title = PostTitle("테스트 제목"),
                    body = PostBody("테스트 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."),
                    status = PostStatus.REVIEWED,
                    createdAt = now,
                )

            reviewedPost.isDraft() shouldBe false
            reviewedPost.isInReview() shouldBe false
            reviewedPost.isReviewed() shouldBe true
        }
    }

    describe("Post 동등성") {

        val now = Instant.now()

        it("같은 ID를 가진 Post는 동등하다") {
            val post1 =
                Post.create(
                    id = PostId(1L),
                    title = PostTitle("제목1"),
                    body = PostBody("내용1입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."),
                    createdAt = now,
                )

            val post2 =
                Post.create(
                    id = PostId(1L),
                    title = PostTitle("제목2"),
                    body = PostBody("내용2입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."),
                    createdAt = now.plusSeconds(60),
                )

            post1 shouldBe post2
            post1.hashCode() shouldBe post2.hashCode()
        }

        it("다른 ID를 가진 Post는 동등하지 않다") {
            val post1 =
                Post.create(
                    id = PostId(1L),
                    title = PostTitle("같은 제목"),
                    body = PostBody("같은 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."),
                    createdAt = now,
                )

            val post2 =
                Post.create(
                    id = PostId(2L),
                    title = PostTitle("같은 제목"),
                    body = PostBody("같은 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 테스트 게시글입니다."),
                    createdAt = now,
                )

            post1 shouldNotBe post2
        }
    }

    describe("Post toString") {

        it("Post 정보를 문자열로 출력한다") {
            val now = Instant.now()
            val post =
                Post.create(
                    id = PostId(1L),
                    title = PostTitle("테스트 제목"),
                    body = PostBody("테스트 내용입니다. 30자 이상의 내용이 필요합니다. 이것은 매우 긴 테스트 게시글입니다."),
                    createdAt = now,
                )

            val str = post.toString()
            str shouldContain "Post(id='1'"
            str shouldContain "title=테스트 제목"
            str shouldContain "body=테스트 내용입니다. 30자 이상의 내용이 필요합니"
            str shouldContain "status=DRAFT"
        }
    }
})
