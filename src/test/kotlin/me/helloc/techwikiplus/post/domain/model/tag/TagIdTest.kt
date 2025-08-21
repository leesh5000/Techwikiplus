package me.helloc.techwikiplus.post.domain.model.tag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TagIdTest : DescribeSpec({

    describe("TagId 생성") {

        context("유효한 ID인 경우") {
            it("양수로 정상 생성된다") {
                val tagId = TagId(1L)
                tagId.value shouldBe 1L
            }

            it("큰 양수로도 정상 생성된다") {
                val tagId = TagId(Long.MAX_VALUE)
                tagId.value shouldBe Long.MAX_VALUE
            }

            it("Snowflake ID 형식의 큰 숫자도 정상 생성된다") {
                val snowflakeId = 1234567890123456789L
                val tagId = TagId(snowflakeId)
                tagId.value shouldBe snowflakeId
            }
        }

        context("유효하지 않은 ID인 경우") {
            it("0이면 예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    TagId(0L)
                }
                exception.message shouldBe "TagId must be positive"
            }

            it("음수이면 예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    TagId(-1L)
                }
                exception.message shouldBe "TagId must be positive"
            }

            it("Long 최소값이면 예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    TagId(Long.MIN_VALUE)
                }
                exception.message shouldBe "TagId must be positive"
            }
        }
    }

    describe("TagId 동등성") {
        it("같은 값을 가진 TagId는 동일하다") {
            val tagId1 = TagId(123L)
            val tagId2 = TagId(123L)
            
            tagId1 shouldBe tagId2
            tagId1.hashCode() shouldBe tagId2.hashCode()
        }

        it("다른 값을 가진 TagId는 동일하지 않다") {
            val tagId1 = TagId(123L)
            val tagId2 = TagId(456L)
            
            tagId1 shouldNotBe tagId2
            tagId1.hashCode() shouldNotBe tagId2.hashCode()
        }

        it("자기 자신과는 항상 동일하다") {
            val tagId = TagId(123L)
            
            tagId shouldBe tagId
        }

        it("null과는 동일하지 않다") {
            val tagId = TagId(123L)
            
            (tagId.equals(null)) shouldBe false
        }

        it("다른 타입의 객체와는 동일하지 않다") {
            val tagId = TagId(123L)
            val other = "123"
            
            (tagId.equals(other)) shouldBe false
        }
    }

    describe("TagId toString") {
        it("ID 값을 문자열로 반환한다") {
            val tagId = TagId(12345L)
            tagId.toString() shouldBe "12345"
        }

        it("큰 숫자도 문자열로 반환한다") {
            val snowflakeId = 1234567890123456789L
            val tagId = TagId(snowflakeId)
            tagId.toString() shouldBe "1234567890123456789"
        }
    }
})