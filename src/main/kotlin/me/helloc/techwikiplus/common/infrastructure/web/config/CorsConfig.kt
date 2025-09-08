package me.helloc.techwikiplus.common.infrastructure.web.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * CORS 설정 구성 클래스
 *
 * CorsProperties를 기반으로 Spring Security에서 사용할
 * CorsConfigurationSource Bean을 생성합니다.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class CorsConfig(
    private val corsProperties: CorsProperties,
) {
    /**
     * CORS 설정 소스를 생성합니다.
     *
     * Spring Security에서 이 Bean을 사용하여 CORS 정책을 적용합니다.
     * allowCredentials가 true이고 allowedOrigins가 "*"인 경우
     * 보안상 문제가 있을 수 있으므로 주의가 필요합니다.
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                // allowCredentials가 true인 경우 "*"를 사용할 수 없음
                if (corsProperties.allowCredentials && corsProperties.allowedOrigins.contains("*")) {
                    // allowCredentials가 true일 때는 명시적인 출처를 사용해야 함
                    // 개발 환경에서는 일반적인 로컬 개발 서버 포트들을 허용
                    allowedOriginPatterns = listOf("*") // 패턴 사용
                } else {
                    allowedOrigins = corsProperties.allowedOrigins
                }

                allowedMethods = corsProperties.allowedMethods
                allowedHeaders = corsProperties.allowedHeaders
                exposedHeaders = corsProperties.exposedHeaders
                allowCredentials = corsProperties.allowCredentials
                maxAge = corsProperties.maxAge
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
