package org.horiga.study.armeria.grpc

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.horiga.study.armeria.grpc.v1.Message
import org.horiga.study.armeria.grpc.v1.ReactorTestServiceGrpc
import org.horiga.study.armeria.grpc.v1.Service.SelectRequest
import org.horiga.study.armeria.grpc.v1.Service.SelectResponse
import org.horiga.study.armeria.repository.TestR2dbcRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class TestService(
    val r2dbcRepository: TestR2dbcRepository
) : ReactorTestServiceGrpc.TestServiceImplBase() {
    companion object {
        val log = LoggerFactory.getLogger(TestService::class.java)!!
    }

    @Throws(StatusRuntimeException::class)
    override fun select(
        request: Mono<SelectRequest>
    ): Mono<SelectResponse> = request.flatMap { thisRequest ->
        if (thisRequest.type == Message.MessageTypes.UNRECOGNIZED ||
            thisRequest.type == Message.MessageTypes.UNSPECIFIED
        ) {
            return@flatMap Mono.error(
                StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("invalid type values"))
            )
        }
        r2dbcRepository.findByTypes(thisRequest.type.name.toLowerCase())
            .timeout(Duration.ofMillis(3000))
            .switchIfEmpty(Flux.empty())
            .map { it.toMessage() }
            .doOnError { err -> log.error("failed to select from test table. type=${thisRequest.type}", err) }
            .onErrorResume { err ->
                log.error("handle errors!!", err)
                val status = when {
                    err is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription("hoge")
                    else -> Status.UNKNOWN.withDescription(err.message)
                }
                Mono.error(StatusRuntimeException(status))
            }
            .collectList()
            .map { items ->
                SelectResponse.newBuilder()
                    .setFilterType(thisRequest.type)
                    .addAllItems(items)
                    .build()
            }
    }
}