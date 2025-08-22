package me.helloc.techwikiplus.common.infrastructure.security.userdetails

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import me.helloc.techwikiplus.user.domain.model.Email
import me.helloc.techwikiplus.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.user.domain.model.Nickname
import me.helloc.techwikiplus.user.domain.model.User
import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.model.UserRole
import me.helloc.techwikiplus.user.domain.model.UserStatus
import me.helloc.techwikiplus.user.domain.service.port.UserRepository
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.time.Instant

class CustomUserDetailsServiceTest : DescribeSpec({

    lateinit var userRepository: UserRepository
    lateinit var userDetailsService: CustomUserDetailsService

    beforeEach {
        userRepository = mockk()
        userDetailsService = CustomUserDetailsService(userRepository)
    }

    describe("CustomUserDetailsService") {
        context("사용자 ID로 조회할 때") {
            it("존재하는 사용자의 UserDetails를 반환해야 함") {
                // given
                val userId = 123L
                val userIdString = userId.toString()
                val user =
                    User(
                        id = UserId(userId),
                        email = Email("test@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("TestUser"),
                        role = UserRole.USER,
                        status = UserStatus.ACTIVE,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    )

                every { userRepository.findBy(any<UserId>()) } returns user

                // when
                val userDetails = userDetailsService.loadUserByUsername(userIdString)

                // then
                userDetails shouldNotBe null
                userDetails.username shouldBe userIdString
                userDetails.password shouldBe "encoded_password"
                userDetails.isEnabled shouldBe true
                userDetails.isAccountNonExpired shouldBe true
                userDetails.isAccountNonLocked shouldBe true
                userDetails.isCredentialsNonExpired shouldBe true
                userDetails.authorities.size shouldBe 1
                userDetails.authorities.first().authority shouldBe "ROLE_USER"
            }

            it("ADMIN 권한을 가진 사용자의 경우 ROLE_ADMIN을 반환해야 함") {
                // given
                val userId = 456L
                val userIdString = userId.toString()
                val admin =
                    User(
                        id = UserId(userId),
                        email = Email("admin@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("Admin"),
                        role = UserRole.ADMIN,
                        status = UserStatus.ACTIVE,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    )

                every { userRepository.findBy(any<UserId>()) } returns admin

                // when
                val userDetails = userDetailsService.loadUserByUsername(userIdString)

                // then
                userDetails.authorities.first().authority shouldBe "ROLE_ADMIN"
            }

            it("DELETED 상태의 사용자는 비활성화되어야 함") {
                // given
                val userId = 789L
                val userIdString = userId.toString()
                val deletedUser =
                    User(
                        id = UserId(userId),
                        email = Email("deleted@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("Deleted"),
                        role = UserRole.USER,
                        status = UserStatus.DELETED,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    )

                every { userRepository.findBy(any<UserId>()) } returns deletedUser

                // when
                val userDetails = userDetailsService.loadUserByUsername(userIdString)

                // then
                userDetails.isEnabled shouldBe false
            }

            it("BANNED 상태의 사용자는 계정이 잠겨있어야 함") {
                // given
                val userId = 999L
                val userIdString = userId.toString()
                val bannedUser =
                    User(
                        id = UserId(userId),
                        email = Email("banned@example.com"),
                        encodedPassword = EncodedPassword("encoded_password"),
                        nickname = Nickname("Banned"),
                        role = UserRole.USER,
                        status = UserStatus.BANNED,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    )

                every { userRepository.findBy(any<UserId>()) } returns bannedUser

                // when
                val userDetails = userDetailsService.loadUserByUsername(userIdString)

                // then
                userDetails.isAccountNonLocked shouldBe false
            }

            it("존재하지 않는 사용자의 경우 UsernameNotFoundException을 던져야 함") {
                // given
                val userIdString = "99999"

                every { userRepository.findBy(any<UserId>()) } returns null

                // when & then
                shouldThrow<UsernameNotFoundException> {
                    userDetailsService.loadUserByUsername(userIdString)
                }
            }
        }
    }
})
