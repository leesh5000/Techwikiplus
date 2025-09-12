plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    // RestDocs API Spec - Spring REST Docs와 OpenAPI 통합
    id("com.epages.restdocs-api-spec") version "0.19.0"
    // ktlint: Kotlin 코드 스타일 검사 및 포맷팅 도구
    // - ./gradlew ktlintCheck: 코드 스타일 위반 검사
    // - ./gradlew ktlintFormat: 자동 코드 포맷팅
    // - ./gradlew addKtlintCheckGitPreCommitHook: Git 커밋 전 자동 검사 설정
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

group = "me.helloc"
version = "0.0.1-SNAPSHOT"
description = "techwikiplus"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin reflection - Spring Boot와 Kotlin data class 사용 시 필수
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Jackson Kotlin 모듈 - JSON 직렬화/역직렬화
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Spring Boot Web - REST API 개발
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Boot Data JPA - 데이터베이스 연동
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Spring Security - 인증/인가 및 비밀번호 암호화
    implementation("org.springframework.boot:spring-boot-starter-security")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // DB
    runtimeOnly("com.mysql:mysql-connector-j")

    // Flyway - 데이터베이스 마이그레이션
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // Spring Boot 테스트 지원
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Spring Security 테스트 지원
    testImplementation("org.springframework.security:spring-security-test")
    // WebTestClient를 위한 WebFlux 의존성
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    // TestContainers 핵심 라이브러리
    testImplementation("org.testcontainers:testcontainers")
    // JUnit5 통합을 위한 TestContainers 확장
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:junit-jupiter")

    // kotest
    testImplementation("io.kotest:kotest-runner-junit5:5.7.2")
    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
    testImplementation("io.kotest:kotest-property:5.7.2")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")

    // ArchUnit: 아키텍처 테스트
    testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")

    // MySQL 컨테이너 - JPA Repository 통합 테스트용
    testImplementation("org.testcontainers:mysql")

    // Spring REST Docs
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")

    // RestDocs API Spec - Spring REST Docs와 OpenAPI 통합
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.19.0")

    // Swagger UI - OpenAPI 문서 시각화
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // MailSender - 이메일 전송 기능
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Redis - 캐시 저장소
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // Kotlin Coroutines - 병렬 처리를 위한 코루틴
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // MockK - Kotlin mocking library for testing
    testImplementation("io.mockk:mockk:1.13.8")

    // Spring Boot Actuator (헬스체크, 메트릭)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Micrometer Prometheus - 프로메테우스 메트릭 노출
    implementation("io.micrometer:micrometer-registry-prometheus")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Spring Boot 메인 클래스 지정
springBoot {
    mainClass.set("me.helloc.techwikiplus.TechwikiplusApplicationKt")
}

ktlint {
    version.set("1.0.1")
    verbose.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }

    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

// Git pre-commit hook 설치 태스크
tasks.register<Exec>("installGitHook") {
    description = "Install pre-commit git hook for ktlint"
    group = "git hooks"
    commandLine("sh", "-c", "./gradlew addKtlintCheckGitPreCommitHook")
}

// OpenAPI 3.0.1 문서 생성 설정
//
// 📌 중요: OpenAPI 문서 관리 정책
// - 생성된 openapi3.yml 파일은 Git에 커밋되어 관리됨
// - Docker 빌드 시 성능 최적화를 위해 테스트를 건너뛰므로 (-x test)
//   사전에 생성된 문서가 JAR에 포함되어야 함
// - API 변경 시 개발자는 반드시:
//   1. ./gradlew test 실행하여 문서 재생성
//   2. src/main/resources/static/api-docs/openapi3.yml 파일 커밋
// - 자세한 내용은 src/main/resources/static/api-docs/README.md 참조
openapi3 {
    // 서버 설정 - 기본값 사용 (http://localhost)
    // TODO: 다중 서버 설정 구현 필요
    setServer("http://localhost:9000")

    title = "TechWikiPlus API"
    description = "TechWikiPlus API Documentation"
    version = System.getenv("IMAGE_TAG") ?: "LOCAL_VERSION"
    format = "yml"
    outputDirectory = "build/api-spec"
    snippetsDirectory = "build/generated-snippets"
}

// OpenAPI 문서에 다중 서버 설정을 추가하는 태스크
tasks.register("updateOpenApiServers") {
    dependsOn("openapi3")
    doLast {
        val openApiFile = file("build/api-spec/openapi3.yml")
        if (openApiFile.exists()) {
            val content = openApiFile.readText()
            // 단일 서버를 다중 서버로 교체
            val updatedContent =
                content.replace(
                    Regex("servers:\\s*\\n\\s*- url: http://localhost:9000"),
                    """servers:
  - url: http://localhost:9000
    description: Local server
  - url: http://13.124.188.47:9000
    description: Production server""",
                )
            openApiFile.writeText(updatedContent)
        }
    }
}

// OpenAPI 문서를 정적 리소스로 복사하는 태스크
// 테스트 실행 후 자동으로 실행되어 문서를 리소스 디렉토리에 복사
// 개발자는 이 파일을 Git에 커밋해야 함
tasks.register<Copy>("copyOpenApiToResources") {
    dependsOn("updateOpenApiServers")
    from("build/api-spec/openapi3.yml")
    into("src/main/resources/static/api-docs")

    // 복사 전 대상 디렉토리 정리
    doFirst {
        delete("src/main/resources/static/api-docs/openapi3.yml")
    }
}

// 테스트 설정
tasks.test {
    useJUnitPlatform()

    // 테스트 실행 전 기존 스니펫만 정리
    // OpenAPI 문서는 삭제하지 않고 누적되도록 함
    doFirst {
        delete("build/api-spec")
        delete("build/generated-snippets")
        // 최종 문서는 삭제하지 않음 - openapi3 태스크가 병합 처리
    }

    // Java 21+ 경고 메시지 제거
    jvmArgs(
        // 동적 에이전트 로딩 명시적 허용
        "-XX:+EnableDynamicAgentLoading",
        // 클래스 데이터 공유 비활성화로 bootstrap classpath 경고 제거
        "-Xshare:off",
    )

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    // 테스트 실행 후 자동으로 OpenAPI 문서 생성 및 복사
    // openapi3 태스크를 명시적으로 실행하도록 수정
    finalizedBy("openapi3", "copyOpenApiToResources")
}
