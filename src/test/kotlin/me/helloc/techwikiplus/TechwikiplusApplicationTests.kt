package me.helloc.techwikiplus

import me.helloc.techwikiplus.common.config.testcontainers.TestContainersInitializer
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ActiveProfiles("e2e-test")
@ContextConfiguration(initializers = [TestContainersInitializer::class])
class TechwikiplusApplicationTests {
    @Test
    fun contextLoads() {
    }
}
