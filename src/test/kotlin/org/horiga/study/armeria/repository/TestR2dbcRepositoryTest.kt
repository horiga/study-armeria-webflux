package org.horiga.study.armeria.repository

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier

@SpringBootTest
@ActiveProfiles("test")
@WithR2dbcMySQLContainer
class TestR2dbcRepositoryTest {

    companion object {
        val log = LoggerFactory.getLogger(TestR2dbcRepositoryTest::class.java)!!
    }

    @Autowired
    lateinit var repository: TestR2dbcRepository

    @Test
    fun `test for r2dbc with docker container`() {

        log.debug("starting JUnit test")

        StepVerifier.create(
            repository.findByTypes("general")
        )
            .expectNextCount(2)
            .expectComplete()
            .verify()
    }

}