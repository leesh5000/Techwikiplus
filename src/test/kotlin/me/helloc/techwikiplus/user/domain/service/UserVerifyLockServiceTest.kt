package me.helloc.techwikiplus.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.helloc.techwikiplus.common.infrastructure.NoOpLockManager
import me.helloc.techwikiplus.common.infrastructure.SlowLockManager
import me.helloc.techwikiplus.common.infrastructure.TestLockManager
import me.helloc.techwikiplus.common.infrastructure.TrackingLockManager
import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.service.port.LockManagerException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class UserVerifyLockServiceTest : DescribeSpec({

    describe("UserVerifyLockService") {

        context("executeWithVerificationLock 메소드는") {

            it("주어진 action을 실행하고 결과를 반환해야 한다") {
                // Given
                val lockManager = NoOpLockManager()
                val service = UserVerifyLockService(lockManager)
                val email = Email("test@example.com")
                val expectedResult = "Success"

                // When
                val result =
                    service.executeWithVerificationLock(email) {
                        expectedResult
                    }

                // Then
                result shouldBe expectedResult
            }

            it("이메일별로 고유한 락 키를 생성해야 한다") {
                // Given
                val lockManager = TrackingLockManager()
                val service = UserVerifyLockService(lockManager)
                val email1 = Email("user1@example.com")
                val email2 = Email("user2@example.com")

                // When
                service.executeWithVerificationLock(email1) { "result1" }
                service.executeWithVerificationLock(email2) { "result2" }

                // Then
                lockManager.getLockCount("user:verify:user1@example.com") shouldBe 1
                lockManager.getLockCount("user:verify:user2@example.com") shouldBe 1
            }

            it("action 실행 중 발생한 예외를 그대로 전파해야 한다") {
                // Given
                val lockManager = NoOpLockManager()
                val service = UserVerifyLockService(lockManager)
                val email = Email("test@example.com")
                val expectedException = RuntimeException("Action failed")

                // When & Then
                val exception =
                    shouldThrow<RuntimeException> {
                        service.executeWithVerificationLock(email) {
                            throw expectedException
                        }
                    }

                exception.message shouldBe "Action failed"
            }

            it("락 획득에 실패하면 LockManagerException을 전파해야 한다") {
                // Given
                val lockManager = SlowLockManager() // 항상 타임아웃되는 락 매니저
                val service = UserVerifyLockService(lockManager)
                val email = Email("test@example.com")

                // When & Then
                val exception =
                    shouldThrow<LockManagerException> {
                        service.executeWithVerificationLock(email) {
                            "This should not be executed"
                        }
                    }

                exception.message shouldContain "Lock acquisition timeout"
            }

            it("동시에 같은 이메일로 요청이 들어와도 순차적으로 처리해야 한다") {
                // Given
                val lockManager = TestLockManager() // synchronized를 사용하는 락 매니저
                val service = UserVerifyLockService(lockManager)
                val email = Email("concurrent@example.com")
                val counter = AtomicInteger(0)
                val results = mutableListOf<Int>()
                val threadCount = 10
                val latch = CountDownLatch(threadCount)
                val executor = Executors.newFixedThreadPool(threadCount)

                // When
                repeat(threadCount) { index ->
                    executor.submit {
                        try {
                            service.executeWithVerificationLock(email) {
                                val value = counter.incrementAndGet()
                                Thread.sleep(10) // 동시성 문제를 더 잘 드러내기 위한 지연
                                synchronized(results) {
                                    results.add(value)
                                }
                                value
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then
                results.sorted() shouldBe (1..threadCount).toList()
                counter.get() shouldBe threadCount
            }

            it("다른 이메일에 대한 요청은 동시에 처리할 수 있어야 한다") {
                // Given
                val lockManager = TestLockManager()
                val service = UserVerifyLockService(lockManager)
                val emails = List(5) { Email("user$it@example.com") }
                val results = mutableMapOf<String, Long>()
                val latch = CountDownLatch(emails.size)
                val executor = Executors.newFixedThreadPool(emails.size)

                // When
                emails.forEach { email ->
                    executor.submit {
                        val startTime = System.currentTimeMillis()
                        service.executeWithVerificationLock(email) {
                            Thread.sleep(50) // 작업 시뮬레이션
                        }
                        val duration = System.currentTimeMillis() - startTime
                        synchronized(results) {
                            results[email.value] = duration
                        }
                        latch.countDown()
                    }
                }

                latch.await()
                executor.shutdown()

                // Then
                // 다른 이메일들이 동시에 처리되었다면, 전체 시간은 개별 시간의 합보다 훨씬 작아야 함
                val maxDuration = results.values.maxOrNull() ?: 0L
                maxDuration shouldBeLessThan 250L // 50ms * 5 = 250ms보다 작아야 함
            }
        }
    }
})
