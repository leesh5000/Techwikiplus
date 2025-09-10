package me.helloc.techwikiplus.common.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("아키텍처 테스트")
class ArchitectureTest {
    private lateinit var importedClasses: JavaClasses

    @BeforeEach
    fun setup() {
        importedClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("me.helloc.techwikiplus.user")
    }

    @Nested
    @DisplayName("의존 방향 규칙")
    inner class LayeredArchitectureRules {
        @Test
        @DisplayName("domain 레이어는 외부 의존성을 가질 수 없다")
        fun domainLayerShouldNotHaveExternalDependencies() {
            val rule =
                noClasses()
                    .that().resideInAPackage("..domain..")
                    .and().doNotHaveFullyQualifiedName("..domain.exception..")
                    .should().accessClassesThat()
                    .resideInAnyPackage(
                        "..interfaces..",
                        "..application..",
                        "..infrastructure..",
                        "org.springframework..",
                        "javax.persistence..",
                        "jakarta.persistence..",
                    )

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("interfaces 레이어는 infrastructure를 직접 참조할 수 없다")
        fun interfacesLayerShouldNotDirectlyAccessInfrastructure() {
            val rule =
                noClasses()
                    .that().resideInAPackage("..interfaces..")
                    .and().doNotHaveFullyQualifiedName("..interfaces.web..")
                    .should().accessClassesThat()
                    .resideInAPackage("..infrastructure..")

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("infrastructure 레이어는 다른 레이어에서 참조될 수 없다")
        fun infrastructureLayerShouldNotBeAccessedByOtherLayers() {
            val rule =
                noClasses()
                    .that().resideInAPackage("..domain..")
                    .or().resideInAPackage("..application..")
                    .should().accessClassesThat()
                    .resideInAPackage("..infrastructure..")

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("domain 레이어는 다른 레이어를 참조할 수 없다")
        fun domainLayerShouldNotAccessOtherLayers() {
            val rule =
                noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().accessClassesThat()
                    .resideInAnyPackage(
                        "..application..",
                        "..interfaces..",
                        "..infrastructure..",
                    )

            rule.check(importedClasses)
        }
    }

    @Nested
    @DisplayName("도메인 모델 규칙")
    inner class DomainModelRules {
        @Test
        @DisplayName("도메인 모델은 불변이어야 한다")
        fun domainModelsShouldBeImmutable() {
            val rule =
                classes()
                    .that().resideInAPackage("..domain.model..")
                    .and().areNotEnums()
                    .and().doNotHaveSimpleName("Companion")
                    .should().haveOnlyFinalFields()

            rule.check(importedClasses)
        }

        @Test
        @DisplayName("도메인 exception은 도메인 패키지 내에 있어야 한다")
        fun domainExceptionsShouldBeInDomainPackage() {
            val rule =
                classes()
                    .that().haveSimpleNameEndingWith("DomainException")
                    .or().haveSimpleNameEndingWith("ErrorCode")
                    .should().resideInAPackage("..domain.exception..")

            rule.check(importedClasses)
        }
    }

    @Nested
    @DisplayName("컨트롤러 규칙")
    inner class ControllerRules {
        @Test
        @DisplayName("컨트롤러는 interfaces.web 패키지에 있어야 한다")
        fun controllersShouldBeInInterfacesWebPackage() {
            val rule =
                classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .or().areAnnotatedWith("org.springframework.stereotype.Controller")
                    .should().resideInAPackage("..interfaces.web..")

            rule.check(importedClasses)
        }
    }

    @Nested
    @DisplayName("서비스 규칙")
    inner class ServiceRules {
        @Test
        @DisplayName("도메인 서비스는 domain.service 패키지에 있어야 한다")
        fun domainServicesShouldBeInDomainServicePackage() {
            val rule =
                classes()
                    .that().haveSimpleNameEndingWith("Service")
                    .and().resideInAPackage("..domain..")
                    .should().resideInAPackage("..domain.service..")

            rule.check(importedClasses)
        }
    }
}
