package me.helloc.techwikiplus.post.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode

class PostTitleTest : DescribeSpec({

    describe("PostTitle 생성") {

        context("유효한 제목인 경우") {
            it("정상적으로 생성된다") {
                val title = PostTitle("테스트 게시글 제목")
                title.value shouldBe "테스트 게시글 제목"
            }

            it("앞뒤 공백을 제거하고 생성된다") {
                val title = PostTitle("  테스트 제목  ")
                title.value shouldBe "테스트 제목"
            }

            it("최대 길이(150자)까지 허용된다") {
                val longTitle = "가".repeat(150)
                val title = PostTitle(longTitle)
                title.value shouldBe longTitle
            }

            it("영문, 숫자, 한글, 공백이 포함된 제목을 허용한다") {
                val title = PostTitle("Test 123 테스트 제목")
                title.value shouldBe "Test 123 테스트 제목"
            }

            it("허용된 특수문자가 포함된 제목을 허용한다") {
                val title = PostTitle("제목! 질문? (설명) [참고] #태그 @멘션")
                title.value shouldBe "제목! 질문? (설명) [참고] #태그 @멘션"
            }
        }

        context("빈 제목인 경우") {
            it("빈 문자열이면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        PostTitle("")
                    }
                exception.postErrorCode shouldBe PostErrorCode.BLANK_TITLE
            }

            it("공백만 있으면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        PostTitle("   ")
                    }
                exception.postErrorCode shouldBe PostErrorCode.BLANK_TITLE
            }
        }

        context("제목 길이 제한") {
            it("151자를 초과하면 예외가 발생한다") {
                val longTitle = "가".repeat(151)
                val exception =
                    shouldThrow<PostDomainException> {
                        PostTitle(longTitle)
                    }
                exception.postErrorCode shouldBe PostErrorCode.TITLE_TOO_LONG
                exception.params[0] shouldBe "title"
                exception.params[1] shouldBe 150
            }

            it("200자를 초과하면 예외가 발생한다") {
                val veryLongTitle = "가".repeat(201)
                val exception =
                    shouldThrow<PostDomainException> {
                        PostTitle(veryLongTitle)
                    }
                exception.postErrorCode shouldBe PostErrorCode.TITLE_TOO_LONG
            }
        }

        context("특수문자 제한") {
            it("이모지가 포함되면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        PostTitle("테스트 😀 제목")
                    }
                exception.postErrorCode shouldBe PostErrorCode.TITLE_CONTAINS_INVALID_CHAR
            }

            it("제어 문자가 포함되면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        PostTitle("테스트\u0000제목")
                    }
                exception.postErrorCode shouldBe PostErrorCode.TITLE_CONTAINS_INVALID_CHAR
            }
        }
    }

    describe("PostTitle 동등성") {

        it("같은 값을 가진 PostTitle은 동등하다") {
            val title1 = PostTitle("같은 제목")
            val title2 = PostTitle("같은 제목")

            title1 shouldBe title2
            title1.hashCode() shouldBe title2.hashCode()
        }

        it("다른 값을 가진 PostTitle은 동등하지 않다") {
            val title1 = PostTitle("제목1")
            val title2 = PostTitle("제목2")

            title1 shouldNotBe title2
        }

        it("앞뒤 공백이 있어도 trim 후 같으면 동등하다") {
            val title1 = PostTitle("  제목  ")
            val title2 = PostTitle("제목")

            title1 shouldBe title2
        }
    }

    describe("PostTitle toString") {

        it("PostTitle 형식으로 출력된다") {
            val title = PostTitle("테스트 제목")
            title.toString() shouldBe "PostTitle(value=테스트 제목)"
        }
    }
})
