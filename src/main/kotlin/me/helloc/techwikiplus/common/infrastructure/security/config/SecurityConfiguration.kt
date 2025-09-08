package me.helloc.techwikiplus.common.infrastructure.security.config

import me.helloc.techwikiplus.common.infrastructure.security.jwt.JwtAuthenticationEntryPoint
import me.helloc.techwikiplus.common.infrastructure.security.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfiguration(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val corsConfigurationSource: CorsConfigurationSource,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf ->
                csrf.disable()
            }
            .cors { cors ->
                // CorsConfig에서 정의한 설정 사용
                cors.configurationSource(corsConfigurationSource)
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    // 공개 엔드포인트
                    .requestMatchers(
                        "/api/v1/users/signup",
                        "/api/v1/users/login",
                        "/api/v1/users/verify",
                        "/api/v1/users/verify/resend",
                        "/api/v1/users/refresh",
                        "/api/v1/posts",
                        "/api/v1/posts/{postId}",
                    ).permitAll()
                    // Actuator 엔드포인트 (헬스체크 및 모니터링)
                    .requestMatchers(
                        "/actuator/**",
                    ).permitAll()
                    // Swagger/OpenAPI 문서
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api-docs/**",
                    ).permitAll()
                    // 정적 리소스
                    .requestMatchers(
                        "/static/**",
                        "/resources/**",
                    ).permitAll()
                    // 나머지 모든 요청은 인증 필요
                    .anyRequest().authenticated()
            }
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            }
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }
}
