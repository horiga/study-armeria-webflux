package org.horiga.study.armeria.grpc

import org.assertj.core.api.Assertions
import org.horiga.study.armeria.grpc.v1.Api.MessageTypes
import org.horiga.study.armeria.grpc.v1.Api.SelectRequest
import org.horiga.study.armeria.repository.WithR2dbcMySQLContainer
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier

@SpringBootTest
@WithR2dbcMySQLContainer
@ActiveProfiles("test")
class R2dbcServiceTest {

    companion object {
        val log = LoggerFactory.getLogger(R2dbcServiceTest::class.java)!!
    }

    @Autowired
    lateinit var service: R2dbcService

    @Test
    fun testGrpcService() {
        StepVerifier.create(
                service.select(SelectRequest.newBuilder().setType(MessageTypes.GENERAL).build().toMono())
        ).expectNextMatches { replyMessage ->
            log.info("replyMessage: $replyMessage")
            Assertions.assertThat(replyMessage.itemsList).isNotEmpty()
            Assertions.assertThat(replyMessage.itemsList.size).isEqualTo(2)
            Assertions.assertThat(replyMessage.filterType).isEqualTo(MessageTypes.GENERAL)
            true
        }.expectComplete().verify()
    }
}