package me.helloc.techwikiplus.common.infrastructure.security.jwt

import me.helloc.techwikiplus.user.domain.service.port.TokenManager
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfig {
    @Bean
    fun jwtTokenManager(jwtProperties: JwtProperties): TokenManager {
        return JwtTokenManager(
            secret = jwtProperties.secret,
            accessTokenValidityInSeconds = jwtProperties.accessTokenValidityInSeconds,
            refreshTokenValidityInSeconds = jwtProperties.refreshTokenValidityInSeconds,
        )
    }
}
