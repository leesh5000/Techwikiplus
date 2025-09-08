package me.helloc.techwikiplus.common.infrastructure.web.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * CORS 설정을 위한 Properties 클래스
 *
 * application.yml의 cors 설정을 매핑합니다.
 * 개발 환경에서는 모든 출처를 허용하고,
 * 운영 환경에서는 특정 도메인만 허용하도록 설정할 수 있습니다.
 */
@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    /**
     * 허용할 출처 목록
     * "*"를 사용하면 모든 출처 허용 (개발 환경용)
     * 운영 환경에서는 구체적인 도메인 지정 권장
     */
    val allowedOrigins: List<String> = listOf("*"),
    /**
     * 허용할 HTTP 메서드 목록
     */
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"),
    /**
     * 허용할 헤더 목록
     * "*"를 사용하면 모든 헤더 허용
     */
    val allowedHeaders: List<String> = listOf("*"),
    /**
     * 클라이언트에 노출할 헤더 목록
     * 기본적으로 CORS 정책에 의해 제한된 헤더들을 명시적으로 노출
     */
    val exposedHeaders: List<String> = listOf("Content-Type", "Authorization", "X-Total-Count"),
    /**
     * 인증 정보(쿠키, 인증 헤더 등) 포함 여부
     * true로 설정하면 credentials를 포함한 요청 허용
     * 주의: allowedOrigins가 "*"인 경우 false여야 함
     */
    val allowCredentials: Boolean = true,
    /**
     * preflight 요청 캐시 시간 (초)
     * 브라우저가 OPTIONS 요청 결과를 캐시하는 시간
     */
    val maxAge: Long = 3600,
)
