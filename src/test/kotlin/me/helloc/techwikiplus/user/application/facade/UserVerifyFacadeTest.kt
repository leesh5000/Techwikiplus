package me.helloc.techwikiplus.user.application.facade

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.common.infrastructure.FakeCacheStore
import me.helloc.techwikiplus.common.infrastructure.FakeClockHolder
import me.helloc.techwikiplus.common.infrastructure.FakeMailSender
import me.helloc.techwikiplus.common.infrastructure.FakeUserRepository
import me.helloc.techwikiplus.common.infrastructure.NoOpLockManager
import me.helloc.techwikiplus.common.infrastructure.SlowLockManager
import me.helloc.techwikiplus.common.infrastructure.TestLockManager
import me.helloc.techwikiplus.user.application.UserVerifyFacade
import me.helloc.techwikiplus.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.user.domain.model.Nickname
import me.helloc.techwikiplus.user.domain.model.RegistrationCode
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole
import me.helloc.techwikiplus.user.domain.model.UserStatus
import me.helloc.techwikiplus.user.domain.service.EmailVerifyService
import me.helloc.techwikiplus.user.domain.service.UserModifier
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.domain.service.UserVerifyLockService
import me.helloc.techwikiplus.user.domain.service.port.LockManagerException
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class UserVerifyFacadeTest : DescribeSpec({

    describe("UserVerifyFacade") {
        context("유효한 인증 코드로 인증 시도 시") {
            it("사용자 상태가 ACTIVE로 변경되어야 한다") {
                // given - 각 테스트를 위한 독립적인 환경 구성
                val fakeUserRepository = FakeUserRepository()
                val fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
                val fakeMailSender = FakeMailSender()
                val fakeCacheStore = FakeCacheStore()
                val fakeLockManager = NoOpLockManager()

                val userReader = UserReader(repository = fakeUserRepository)
                val emailVerifyService =
                    EmailVerifyService(
                        mailSender = fakeMailSender,
                        cacheStore = fakeCacheStore,
                    )
                val userModifier =
                    UserModifier(
                        clockHolder = fakeClockHolder,
                        repository = fakeUserRepository,
                    )
                val userVerifyLockService = UserVerifyLockService(fakeLockManager)
                val userVerifyFacade =
                    UserVerifyFacade(
                        userReader = userReader,
                        emailVerifyService = emailVerifyService,
                        userModifier = userModifier,
                        userVerifyLockService = userVerifyLockService,
                    )

                val email = Email("test.active@example.com")
                val pendingUser =
                    User.create(
                        id = UserId(1000001L),
                        email = email,
                        nickname = Nickname("testuser"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        status = UserStatus.PENDING,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                    )
                fakeUserRepository.save(pendingUser)

                val validCode = RegistrationCode("123456")
                val cacheKey = "user:registration_code:${email.value}"
                fakeCacheStore.put(cacheKey, validCode.value)

                // when
                userVerifyFacade.handle(email, validCode)

                // then
                val activatedUser = fakeUserRepository.findBy(email)
                activatedUser?.status shouldBe UserStatus.ACTIVE
                activatedUser?.isActive() shouldBe true
                activatedUser?.modifiedAt shouldBe fakeClockHolder.now()
            }
        }

        context("잘못된 인증 코드로 인증 시도 시") {
            it("CODE_MISMATCH 예외가 발생하고 사용자는 PENDING 상태를 유지해야 한다") {
                // given - 독립적인 환경 구성
                val fakeUserRepository = FakeUserRepository()
                val fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
                val fakeMailSender = FakeMailSender()
                val fakeCacheStore = FakeCacheStore()
                val fakeLockManager = NoOpLockManager()

                val userReader = UserReader(repository = fakeUserRepository)
                val emailVerifyService =
                    EmailVerifyService(
                        mailSender = fakeMailSender,
                        cacheStore = fakeCacheStore,
                    )
                val userModifier =
                    UserModifier(
                        clockHolder = fakeClockHolder,
                        repository = fakeUserRepository,
                    )
                val userVerifyLockService = UserVerifyLockService(fakeLockManager)
                val userVerifyFacade =
                    UserVerifyFacade(
                        userReader = userReader,
                        emailVerifyService = emailVerifyService,
                        userModifier = userModifier,
                        userVerifyLockService = userVerifyLockService,
                    )

                val email = Email("test.mismatch@example.com")
                val pendingUser =
                    User.create(
                        id = UserId(1000002L),
                        email = email,
                        nickname = Nickname("testuser2"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        status = UserStatus.PENDING,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                    )
                fakeUserRepository.save(pendingUser)

                val validCode = RegistrationCode("123456")
                val invalidCode = RegistrationCode("999999")
                val cacheKey = "user:registration_code:${email.value}"
                fakeCacheStore.put(cacheKey, validCode.value)

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userVerifyFacade.handle(email, invalidCode)
                    }

                exception.userErrorCode shouldBe UserErrorCode.CODE_MISMATCH

                val user = fakeUserRepository.findBy(email)
                user?.status shouldBe UserStatus.PENDING
            }
        }

        context("PENDING 상태가 아닌 사용자가 인증 시도 시") {
            it("NOT_FOUND_PENDING_USER 예외가 발생해야 한다") {
                // given - 독립적인 환경 구성
                val fakeUserRepository = FakeUserRepository()
                val fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
                val fakeMailSender = FakeMailSender()
                val fakeCacheStore = FakeCacheStore()
                val fakeLockManager = NoOpLockManager()

                val userReader = UserReader(repository = fakeUserRepository)
                val emailVerifyService =
                    EmailVerifyService(
                        mailSender = fakeMailSender,
                        cacheStore = fakeCacheStore,
                    )
                val userModifier =
                    UserModifier(
                        clockHolder = fakeClockHolder,
                        repository = fakeUserRepository,
                    )
                val userVerifyLockService = UserVerifyLockService(fakeLockManager)
                val userVerifyFacade =
                    UserVerifyFacade(
                        userReader = userReader,
                        emailVerifyService = emailVerifyService,
                        userModifier = userModifier,
                        userVerifyLockService = userVerifyLockService,
                    )

                val email = Email("test.already.active@example.com")
                val activeUser =
                    User.create(
                        id = UserId(1000003L),
                        email = email,
                        nickname = Nickname("testuser3"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        status = UserStatus.ACTIVE,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                    )
                fakeUserRepository.save(activeUser)

                val code = RegistrationCode("123456")

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userVerifyFacade.handle(email, code)
                    }

                exception.userErrorCode shouldBe UserErrorCode.NOT_FOUND_PENDING_USER
                exception.params shouldBe arrayOf(email.value)
            }
        }

        context("인증 코드가 만료되었거나 캐시에 없을 때") {
            it("REGISTRATION_EXPIRED 예외가 발생해야 한다") {
                // given - 독립적인 환경 구성
                val fakeUserRepository = FakeUserRepository()
                val fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
                val fakeMailSender = FakeMailSender()
                val fakeCacheStore = FakeCacheStore()
                val fakeLockManager = NoOpLockManager()

                val userReader = UserReader(repository = fakeUserRepository)
                val emailVerifyService =
                    EmailVerifyService(
                        mailSender = fakeMailSender,
                        cacheStore = fakeCacheStore,
                    )
                val userModifier =
                    UserModifier(
                        clockHolder = fakeClockHolder,
                        repository = fakeUserRepository,
                    )
                val userVerifyLockService = UserVerifyLockService(fakeLockManager)
                val userVerifyFacade =
                    UserVerifyFacade(
                        userReader = userReader,
                        emailVerifyService = emailVerifyService,
                        userModifier = userModifier,
                        userVerifyLockService = userVerifyLockService,
                    )

                val email = Email("test.expired@example.com")
                val pendingUser =
                    User.create(
                        id = UserId(1000004L),
                        email = email,
                        nickname = Nickname("testuser4"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        status = UserStatus.PENDING,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                    )
                fakeUserRepository.save(pendingUser)

                val code = RegistrationCode("123456")
                // 캐시에 코드를 저장하지 않음

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userVerifyFacade.handle(email, code)
                    }

                exception.userErrorCode shouldBe UserErrorCode.REGISTRATION_EXPIRED

                val user = fakeUserRepository.findBy(email)
                user?.status shouldBe UserStatus.PENDING
            }
        }

        context("인증이 성공적으로 완료되면") {
            it("캐시에서 인증 코드가 삭제되어야 한다") {
                // given - 독립적인 환경 구성
                val fakeUserRepository = FakeUserRepository()
                val fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
                val fakeMailSender = FakeMailSender()
                val fakeCacheStore = FakeCacheStore()
                val fakeLockManager = NoOpLockManager()

                val userReader = UserReader(repository = fakeUserRepository)
                val emailVerifyService =
                    EmailVerifyService(
                        mailSender = fakeMailSender,
                        cacheStore = fakeCacheStore,
                    )
                val userModifier =
                    UserModifier(
                        clockHolder = fakeClockHolder,
                        repository = fakeUserRepository,
                    )
                val userVerifyLockService = UserVerifyLockService(fakeLockManager)
                val userVerifyFacade =
                    UserVerifyFacade(
                        userReader = userReader,
                        emailVerifyService = emailVerifyService,
                        userModifier = userModifier,
                        userVerifyLockService = userVerifyLockService,
                    )

                val email = Email("test.cache.delete@example.com")
                val pendingUser =
                    User.create(
                        id = UserId(1000005L),
                        email = email,
                        nickname = Nickname("testuser5"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        status = UserStatus.PENDING,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                    )
                fakeUserRepository.save(pendingUser)

                val validCode = RegistrationCode("123456")
                val cacheKey = "user:registration_code:${email.value}"
                fakeCacheStore.put(cacheKey, validCode.value)

                // when
                userVerifyFacade.handle(email, validCode)

                // then
                fakeCacheStore.contains(cacheKey) shouldBe false
            }
        }

        context("인증 프로세스 실행 시") {
            it("UserReader -> EmailVerifyService -> UserModifier 순서로 실행되어야 한다") {
                // given - 독립적인 환경 구성
                val fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
                val fakeMailSender = FakeMailSender()

                val email = Email("test.order@example.com")
                val pendingUser =
                    User.create(
                        id = UserId(1000006L),
                        email = email,
                        nickname = Nickname("testuser6"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        status = UserStatus.PENDING,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                    )

                val validCode = RegistrationCode("123456")
                val cacheKey = "user:registration_code:${email.value}"

                var userReaderCalled = false
                var emailVerifyCalled = false
                var userModifierCalled = false
                val callOrder = mutableListOf<String>()

                val fakeUserRepository =
                    object : FakeUserRepository() {
                        init {
                            save(pendingUser)
                        }

                        override fun findBy(email: Email): User? {
                            if (!userReaderCalled) {
                                userReaderCalled = true
                                callOrder.add("userReader")
                            }
                            return super.findBy(email)
                        }

                        override fun save(user: User): User {
                            if (user.status == UserStatus.ACTIVE) {
                                userModifierCalled = true
                                callOrder.add("userModifier")
                            }
                            return super.save(user)
                        }
                    }

                val fakeCacheStore =
                    object : FakeCacheStore() {
                        init {
                            put(cacheKey, validCode.value)
                        }

                        override fun get(key: String): String? {
                            if (!emailVerifyCalled) {
                                emailVerifyCalled = true
                                callOrder.add("emailVerify")
                            }
                            return super.get(key)
                        }
                    }

                val userReader = UserReader(repository = fakeUserRepository)
                val emailVerifyService =
                    EmailVerifyService(
                        mailSender = fakeMailSender,
                        cacheStore = fakeCacheStore,
                    )
                val userModifier =
                    UserModifier(
                        clockHolder = fakeClockHolder,
                        repository = fakeUserRepository,
                    )
                val userVerifyLockService = UserVerifyLockService(NoOpLockManager())
                val userVerifyFacade =
                    UserVerifyFacade(
                        userReader = userReader,
                        emailVerifyService = emailVerifyService,
                        userModifier = userModifier,
                        userVerifyLockService = userVerifyLockService,
                    )

                // when
                userVerifyFacade.handle(email, validCode)

                // then
                callOrder shouldBe listOf("userReader", "emailVerify", "userModifier")
                userReaderCalled shouldBe true
                emailVerifyCalled shouldBe true
                userModifierCalled shouldBe true
            }
        }

        context("인증이 완료되면") {
            it("사용자의 수정 시간이 현재 시간으로 업데이트되어야 한다") {
                // given - 독립적인 환경 구성
                val fakeUserRepository = FakeUserRepository()
                val fixedTime = Instant.parse("2025-01-15T10:30:00Z")
                val fakeClockHolder = FakeClockHolder(fixedTime)
                val fakeMailSender = FakeMailSender()
                val fakeCacheStore = FakeCacheStore()
                val fakeLockManager = NoOpLockManager()

                val userReader = UserReader(repository = fakeUserRepository)
                val emailVerifyService =
                    EmailVerifyService(
                        mailSender = fakeMailSender,
                        cacheStore = fakeCacheStore,
                    )
                val userModifier =
                    UserModifier(
                        clockHolder = fakeClockHolder,
                        repository = fakeUserRepository,
                    )
                val userVerifyLockService = UserVerifyLockService(fakeLockManager)
                val userVerifyFacade =
                    UserVerifyFacade(
                        userReader = userReader,
                        emailVerifyService = emailVerifyService,
                        userModifier = userModifier,
                        userVerifyLockService = userVerifyLockService,
                    )

                val email = Email("test.time.update@example.com")
                val createdTime = Instant.parse("2024-12-01T00:00:00Z")
                val pendingUser =
                    User.create(
                        id = UserId(1000007L),
                        email = email,
                        nickname = Nickname("testuser7"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        status = UserStatus.PENDING,
                        role = UserRole.USER,
                        createdAt = createdTime,
                        modifiedAt = createdTime,
                    )
                fakeUserRepository.save(pendingUser)

                val validCode = RegistrationCode("123456")
                val cacheKey = "user:registration_code:${email.value}"
                fakeCacheStore.put(cacheKey, validCode.value)

                // when
                userVerifyFacade.handle(email, validCode)

                // then
                val activatedUser = fakeUserRepository.findBy(email)
                activatedUser?.createdAt shouldBe createdTime
                activatedUser?.modifiedAt shouldBe fixedTime
            }
        }

        context("이미 활성화된 사용자가 재인증 시도 시") {
            it("첫 번째 인증만 성공하고 두 번째는 NOT_FOUND_PENDING_USER 예외가 발생해야 한다") {
                // given - 독립적인 환경 구성
                val fakeUserRepository = FakeUserRepository()
                val fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
                val fakeMailSender = FakeMailSender()
                val fakeCacheStore = FakeCacheStore()
                val fakeLockManager = NoOpLockManager()

                val userReader = UserReader(repository = fakeUserRepository)
                val emailVerifyService =
                    EmailVerifyService(
                        mailSender = fakeMailSender,
                        cacheStore = fakeCacheStore,
                    )
                val userModifier =
                    UserModifier(
                        clockHolder = fakeClockHolder,
                        repository = fakeUserRepository,
                    )
                val userVerifyLockService = UserVerifyLockService(fakeLockManager)
                val userVerifyFacade =
                    UserVerifyFacade(
                        userReader = userReader,
                        emailVerifyService = emailVerifyService,
                        userModifier = userModifier,
                        userVerifyLockService = userVerifyLockService,
                    )

                val email = Email("test.double.activate@example.com")
                val pendingUser =
                    User.create(
                        id = UserId(1000008L),
                        email = email,
                        nickname = Nickname("testuser8"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        status = UserStatus.PENDING,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                    )
                fakeUserRepository.save(pendingUser)

                val validCode = RegistrationCode("123456")
                val cacheKey = "user:registration_code:${email.value}"
                fakeCacheStore.put(cacheKey, validCode.value)

                // when - 첫 번째 인증
                userVerifyFacade.handle(email, validCode)

                // then - 두 번째 인증 시도
                val exception =
                    shouldThrow<UserDomainException> {
                        userVerifyFacade.handle(email, validCode)
                    }

                exception.userErrorCode shouldBe UserErrorCode.NOT_FOUND_PENDING_USER
            }
        }

        context("BANNED 상태의 사용자가 인증 시도 시") {
            it("NOT_FOUND_PENDING_USER 예외가 발생해야 한다") {
                // given - 독립적인 환경 구성
                val fakeUserRepository = FakeUserRepository()
                val fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
                val fakeMailSender = FakeMailSender()
                val fakeCacheStore = FakeCacheStore()
                val fakeLockManager = NoOpLockManager()

                val userReader = UserReader(repository = fakeUserRepository)
                val emailVerifyService =
                    EmailVerifyService(
                        mailSender = fakeMailSender,
                        cacheStore = fakeCacheStore,
                    )
                val userModifier =
                    UserModifier(
                        clockHolder = fakeClockHolder,
                        repository = fakeUserRepository,
                    )
                val userVerifyLockService = UserVerifyLockService(fakeLockManager)
                val userVerifyFacade =
                    UserVerifyFacade(
                        userReader = userReader,
                        emailVerifyService = emailVerifyService,
                        userModifier = userModifier,
                        userVerifyLockService = userVerifyLockService,
                    )

                val email = Email("test.banned@example.com")
                val bannedUser =
                    User.create(
                        id = UserId(1000009L),
                        email = email,
                        nickname = Nickname("testuser9"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        status = UserStatus.BANNED,
                        role = UserRole.USER,
                        createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                        modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                    )
                fakeUserRepository.save(bannedUser)

                val code = RegistrationCode("123456")

                // when & then
                val exception =
                    shouldThrow<UserDomainException> {
                        userVerifyFacade.handle(email, code)
                    }

                exception.userErrorCode shouldBe UserErrorCode.NOT_FOUND_PENDING_USER
                exception.params shouldBe arrayOf(email.value)
            }
        }

        describe("동시성 제어") {
            context("동일 이메일로 동시에 여러 인증 요청이 들어올 때") {
                it("분산 락에 의해 순차적으로 처리되고 첫 번째만 성공해야 한다") {
                    // given - 독립적인 환경 구성
                    val fakeUserRepository = FakeUserRepository()
                    val fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
                    val fakeMailSender = FakeMailSender()
                    val fakeCacheStore = FakeCacheStore()
                    val testLockManager = TestLockManager()
                    val userVerifyLockService = UserVerifyLockService(testLockManager)

                    val email = Email("concurrent.test@example.com")
                    val code = RegistrationCode("123456")
                    val cacheKey = "user:registration_code:${email.value}"

                    // 동시 요청 횟수
                    val threadCount = 10
                    val successCount = AtomicInteger(0)
                    val failureCount = AtomicInteger(0)
                    val latch = CountDownLatch(threadCount)
                    val executor = Executors.newFixedThreadPool(threadCount)

                    // 최초 PENDING 사용자 생성
                    val pendingUser =
                        User.create(
                            id = UserId(2000001L),
                            email = email,
                            nickname = Nickname("concurrent_user"),
                            encodedPassword = EncodedPassword("encoded_password"),
                            status = UserStatus.PENDING,
                            role = UserRole.USER,
                            createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                            modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                        )
                    fakeUserRepository.save(pendingUser)
                    fakeCacheStore.put(cacheKey, code.value)

                    val userVerifyFacade =
                        UserVerifyFacade(
                            userReader = UserReader(fakeUserRepository),
                            emailVerifyService = EmailVerifyService(fakeMailSender, fakeCacheStore),
                            userModifier = UserModifier(fakeClockHolder, fakeUserRepository),
                            userVerifyLockService = userVerifyLockService,
                        )

                    // when
                    repeat(threadCount) {
                        executor.submit {
                            try {
                                userVerifyFacade.handle(email, code)
                                successCount.incrementAndGet()
                            } catch (e: UserDomainException) {
                                failureCount.incrementAndGet()
                            } finally {
                                latch.countDown()
                            }
                        }
                    }

                    latch.await()
                    executor.shutdown()

                    // then
                    // 첫 번째 요청만 성공하고 나머지는 모두 실패
                    successCount.get() shouldBe 1
                    failureCount.get() shouldBe (threadCount - 1)

                    val finalUser = fakeUserRepository.findBy(email)
                    finalUser?.status shouldBe UserStatus.ACTIVE
                }
            }

            context("서로 다른 이메일로 동시에 인증 요청이 들어올 때") {
                it("50개의 동시 요청이 모두 성공해야 한다") {
                    // given
                    val fakeUserRepository = FakeUserRepository()
                    val fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
                    val fakeMailSender = FakeMailSender()
                    val fakeCacheStore = FakeCacheStore()
                    val testLockManager = TestLockManager()
                    val userVerifyLockService = UserVerifyLockService(testLockManager)

                    val userCount = 50
                    val emails = List(userCount) { Email("user$it@example.com") }
                    val code = RegistrationCode("123456")

                    // 50개의 사용자 생성
                    emails.forEachIndexed { index, email ->
                        val user =
                            User.create(
                                id = UserId(3000001L + index),
                                email = email,
                                nickname = Nickname("user$index"),
                                encodedPassword = EncodedPassword("password"),
                                status = UserStatus.PENDING,
                                role = UserRole.USER,
                                createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                                modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                            )
                        fakeUserRepository.save(user)
                        fakeCacheStore.put("user:registration_code:${email.value}", code.value)
                    }

                    val userVerifyFacade =
                        UserVerifyFacade(
                            userReader = UserReader(fakeUserRepository),
                            emailVerifyService = EmailVerifyService(fakeMailSender, fakeCacheStore),
                            userModifier = UserModifier(fakeClockHolder, fakeUserRepository),
                            userVerifyLockService = userVerifyLockService,
                        )

                    // when - 50개 동시 요청
                    val latch = CountDownLatch(userCount)
                    val executor = Executors.newFixedThreadPool(10) // 스레드 풀 크기는 10개로 제한

                    emails.forEach { email ->
                        executor.submit {
                            try {
                                userVerifyFacade.handle(email, code)
                            } finally {
                                latch.countDown()
                            }
                        }
                    }

                    // 최대 30초 대기
                    val completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS)
                    executor.shutdown()

                    // then
                    completed shouldBe true

                    // 모든 사용자가 ACTIVE 상태인지 확인
                    emails.forEach { email ->
                        val user = fakeUserRepository.findBy(email)
                        user?.status shouldBe UserStatus.ACTIVE
                    }
                }
            }

            context("락 획득에 실패할 때") {
                it("LockManagerException이 전파되고 인증이 수행되지 않아야 한다") {
                    // given - 독립적인 환경 구성
                    val fakeUserRepository = FakeUserRepository()
                    val fakeClockHolder = FakeClockHolder(Instant.parse("2025-01-01T00:00:00Z"))
                    val fakeMailSender = FakeMailSender()
                    val fakeCacheStore = FakeCacheStore()
                    val slowLockManager = SlowLockManager() // 항상 타임아웃되는 락 매니저
                    val userVerifyLockService = UserVerifyLockService(slowLockManager)

                    val email = Email("lock.fail@example.com")
                    val pendingUser =
                        User.create(
                            id = UserId(4000001L),
                            email = email,
                            nickname = Nickname("lock_fail_user"),
                            encodedPassword = EncodedPassword("encoded_password"),
                            status = UserStatus.PENDING,
                            role = UserRole.USER,
                            createdAt = Instant.parse("2024-12-01T00:00:00Z"),
                            modifiedAt = Instant.parse("2024-12-01T00:00:00Z"),
                        )
                    fakeUserRepository.save(pendingUser)

                    val code = RegistrationCode("123456")
                    val cacheKey = "user:registration_code:${email.value}"
                    fakeCacheStore.put(cacheKey, code.value)

                    val userVerifyFacade =
                        UserVerifyFacade(
                            userReader = UserReader(fakeUserRepository),
                            emailVerifyService = EmailVerifyService(fakeMailSender, fakeCacheStore),
                            userModifier = UserModifier(fakeClockHolder, fakeUserRepository),
                            userVerifyLockService = userVerifyLockService,
                        )

                    // when & then
                    val exception =
                        shouldThrow<LockManagerException> {
                            userVerifyFacade.handle(email, code)
                        }

                    exception.message shouldNotBe null

                    // 락 획득 실패로 인증이 수행되지 않았으므로 여전히 PENDING 상태
                    val user = fakeUserRepository.findBy(email)
                    user?.status shouldBe UserStatus.PENDING
                }
            }
        }
    }
})
