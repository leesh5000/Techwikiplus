package me.helloc.techwikiplus.post.domain.model.post

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode

class PostBodyTest : DescribeSpec({

    describe("PostBody 생성") {

        context("유효한 내용인 경우") {
            it("정상적으로 생성된다") {
                val body = PostBody("이것은 테스트 게시글 내용입니다. 최소 30자 이상의 내용이 필요합니다.")
                body.value shouldBe "이것은 테스트 게시글 내용입니다. 최소 30자 이상의 내용이 필요합니다."
            }

            it("앞뒤 공백을 제거하고 생성된다") {
                val content = "  이것은 테스트 게시글 내용입니다. 최소 30자 이상의 내용이 필요합니다.  "
                val body = PostBody(content)
                body.value shouldBe "이것은 테스트 게시글 내용입니다. 최소 30자 이상의 내용이 필요합니다."
            }

            it("최소 길이(30자)로 생성할 수 있다") {
                val minContent = "가".repeat(30)
                val body = PostBody(minContent)
                body.value shouldBe minContent
                body.value.length shouldBe 30
            }

            it("최대 길이(50000자)까지 허용된다") {
                val maxContent = "가".repeat(50000)
                val body = PostBody(maxContent)
                body.value shouldBe maxContent
                body.value.length shouldBe 50000
            }

            it("줄바꿈과 특수문자가 포함된 내용을 허용한다") {
                val content =
                    """
                    이것은 여러 줄로 구성된 게시글 내용입니다.
                    
                    두 번째 단락입니다.
                    특수문자도 포함할 수 있습니다: !@#$%^&*()
                    
                    마지막 단락입니다.
                    """.trimIndent()
                val body = PostBody(content)
                body.value shouldContain "두 번째 단락"
                body.value shouldContain "!@#$%^&*()"
            }
        }

        context("빈 내용인 경우") {
            it("빈 문자열이면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        PostBody("")
                    }
                exception.postErrorCode shouldBe PostErrorCode.BLANK_CONTENT
            }

            it("공백만 있으면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        PostBody("   ")
                    }
                exception.postErrorCode shouldBe PostErrorCode.BLANK_CONTENT
            }
        }

        context("내용 길이 제한") {
            it("29자 이하면 예외가 발생한다") {
                val shortContent = "가".repeat(29)
                val exception =
                    shouldThrow<PostDomainException> {
                        PostBody(shortContent)
                    }
                exception.postErrorCode shouldBe PostErrorCode.CONTENT_TOO_SHORT
                exception.params[0] shouldBe "content"
                exception.params[1] shouldBe 30
            }

            it("1자만 있으면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        PostBody("가")
                    }
                exception.postErrorCode shouldBe PostErrorCode.CONTENT_TOO_SHORT
            }

            it("50001자를 초과하면 예외가 발생한다") {
                val longContent = "가".repeat(50001)
                val exception =
                    shouldThrow<PostDomainException> {
                        PostBody(longContent)
                    }
                exception.postErrorCode shouldBe PostErrorCode.CONTENT_TOO_LONG
                exception.params[0] shouldBe "content"
                exception.params[1] shouldBe 50000
            }

            it("100000자를 초과하면 예외가 발생한다") {
                val veryLongContent = "가".repeat(100000)
                val exception =
                    shouldThrow<PostDomainException> {
                        PostBody(veryLongContent)
                    }
                exception.postErrorCode shouldBe PostErrorCode.CONTENT_TOO_LONG
            }
        }

        context("공백 처리 후 길이 검증") {
            it("trim 후 30자 미만이면 예외가 발생한다") {
                val contentWithSpaces = "  " + "가".repeat(29) + "  "
                val exception =
                    shouldThrow<PostDomainException> {
                        PostBody(contentWithSpaces)
                    }
                exception.postErrorCode shouldBe PostErrorCode.CONTENT_TOO_SHORT
            }

            it("trim 후 30자 이상이면 정상 생성된다") {
                val contentWithSpaces = "  " + "가".repeat(30) + "  "
                val body = PostBody(contentWithSpaces)
                body.value shouldBe "가".repeat(30)
            }
        }
    }

    describe("PostBody 동등성") {

        it("같은 값을 가진 PostBody는 동등하다") {
            val content = "이것은 테스트 게시글 내용입니다. 최소 30자 이상의 내용이 필요합니다."
            val body1 = PostBody(content)
            val body2 = PostBody(content)

            body1 shouldBe body2
            body1.hashCode() shouldBe body2.hashCode()
        }

        it("다른 값을 가진 PostBody는 동등하지 않다") {
            val body1 = PostBody("첫 번째 게시글 내용입니다. 최소 30자 이상의 내용이 필요합니다.")
            val body2 = PostBody("두 번째 게시글 내용입니다. 최소 30자 이상의 내용이 필요합니다.")

            body1 shouldNotBe body2
        }

        it("앞뒤 공백이 있어도 trim 후 같으면 동등하다") {
            val content = "이것은 테스트 게시글 내용입니다. 최소 30자 이상의 내용이 필요합니다."
            val body1 = PostBody("  $content  ")
            val body2 = PostBody(content)

            body1 shouldBe body2
        }
    }

    describe("PostBody toString") {

        it("짧은 내용은 전체가 출력된다") {
            val content = "이것은 짧은 게시글 내용입니다. 30자 이상 50자 이하의 적절한 길이입니다."
            val body = PostBody(content)
            body.toString() shouldBe "PostBody(value=$content)"
        }

        it("긴 내용은 50자까지만 출력되고 ...이 붙는다") {
            val content = "가".repeat(100)
            val body = PostBody(content)
            body.toString() shouldBe "PostBody(value=${"가".repeat(50)}...)"
        }

        it("정확히 50자면 ...이 붙지 않는다") {
            val content = "가".repeat(50)
            val body = PostBody(content)
            body.toString() shouldBe "PostBody(value=$content)"
        }
    }
})
