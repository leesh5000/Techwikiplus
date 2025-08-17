package me.helloc.techwikiplus.common.infrastructure.security.userdetails

import me.helloc.techwikiplus.user.domain.model.UserId
import me.helloc.techwikiplus.user.domain.service.port.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user =
            userRepository.findBy(UserId.from(username))
                ?: throw UsernameNotFoundException("User not found with id: $username")

        return CustomUserDetails(user)
    }
}
