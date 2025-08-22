package me.helloc.techwikiplus.post.domain.model.tag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode

class TagNameTest : DescribeSpec({

    describe("TagName 생성") {

        context("유효한 태그명인 경우") {
            it("정상적으로 생성된다") {
                val tagName = TagName("spring")
                tagName.value shouldBe "spring"
            }

            it("앞뒤 공백을 제거하고 소문자로 변환하여 생성된다") {
                val tagName = TagName("  SpringBoot  ")
                tagName.value shouldBe "springboot"
            }

            it("대문자를 소문자로 변환하여 생성된다") {
                val tagName = TagName("KOTLIN")
                tagName.value shouldBe "kotlin"
            }

            it("한글 태그명을 허용한다") {
                val tagName = TagName("스프링부트")
                tagName.value shouldBe "스프링부트"
            }

            it("숫자가 포함된 태그명을 허용한다") {
                val tagName = TagName("java17")
                tagName.value shouldBe "java17"
            }

            it("하이픈이 포함된 태그명을 허용한다") {
                val tagName = TagName("spring-boot")
                tagName.value shouldBe "spring-boot"
            }

            it("언더스코어가 포함된 태그명을 허용한다") {
                val tagName = TagName("test_driven_development")
                tagName.value shouldBe "test_driven_development"
            }

            it("최소 길이(2자)의 태그명을 허용한다") {
                val tagName = TagName("go")
                tagName.value shouldBe "go"
            }

            it("최대 길이(30자)의 태그명을 허용한다") {
                val longTag = "a".repeat(30)
                val tagName = TagName(longTag)
                tagName.value shouldBe longTag
            }

            it("한글, 영문, 숫자, 하이픈, 언더스코어가 혼합된 태그명을 허용한다") {
                val tagName = TagName("한글_english-123")
                tagName.value shouldBe "한글_english-123"
            }
        }

        context("빈 태그명인 경우") {
            it("빈 문자열이면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        TagName("")
                    }
                exception.postErrorCode shouldBe PostErrorCode.BLANK_TAG
            }

            it("공백만 있는 문자열이면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        TagName("   ")
                    }
                exception.postErrorCode shouldBe PostErrorCode.BLANK_TAG
            }
        }

        context("태그명 길이가 유효하지 않은 경우") {
            it("1자이면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        TagName("a")
                    }
                exception.postErrorCode shouldBe PostErrorCode.TAG_TOO_SHORT
            }

            it("31자이면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        TagName("a".repeat(31))
                    }
                exception.postErrorCode shouldBe PostErrorCode.TAG_TOO_LONG
            }
        }

        context("허용되지 않는 문자가 포함된 경우") {
            it("중간에 공백이 포함되면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        TagName("spring boot")
                    }
                exception.postErrorCode shouldBe PostErrorCode.TAG_CONTAINS_INVALID_CHAR
            }

            it("특수문자 @가 포함되면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        TagName("spring@boot")
                    }
                exception.postErrorCode shouldBe PostErrorCode.TAG_CONTAINS_INVALID_CHAR
            }

            it("특수문자 #이 포함되면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        TagName("#spring")
                    }
                exception.postErrorCode shouldBe PostErrorCode.TAG_CONTAINS_INVALID_CHAR
            }

            it("특수문자 !가 포함되면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        TagName("spring!")
                    }
                exception.postErrorCode shouldBe PostErrorCode.TAG_CONTAINS_INVALID_CHAR
            }

            it("점(.)이 포함되면 예외가 발생한다") {
                val exception =
                    shouldThrow<PostDomainException> {
                        TagName("node.js")
                    }
                exception.postErrorCode shouldBe PostErrorCode.TAG_CONTAINS_INVALID_CHAR
            }
        }
    }

    describe("TagName 동등성") {
        it("같은 값을 가진 TagName은 동일하다") {
            val tagName1 = TagName("spring")
            val tagName2 = TagName("spring")

            tagName1 shouldBe tagName2
            tagName1.hashCode() shouldBe tagName2.hashCode()
        }

        it("대소문자가 다르더라도 정규화 후 같으면 동일하다") {
            val tagName1 = TagName("Spring")
            val tagName2 = TagName("SPRING")

            tagName1 shouldBe tagName2
            tagName1.hashCode() shouldBe tagName2.hashCode()
        }

        it("다른 값을 가진 TagName은 동일하지 않다") {
            val tagName1 = TagName("spring")
            val tagName2 = TagName("kotlin")

            tagName1 shouldNotBe tagName2
        }
    }

    describe("TagName toString") {
        it("태그명 값을 문자열로 반환한다") {
            val tagName = TagName("spring-boot")
            tagName.toString() shouldBe "spring-boot"
        }
    }
})
