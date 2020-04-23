package org.horiga.study.armeria.grpc

import io.grpc.Status.INVALID_ARGUMENT
import io.grpc.Status.UNKNOWN
import io.grpc.StatusRuntimeException
import org.horiga.study.armeria.grpc.v1.Message
import org.horiga.study.armeria.grpc.v1.ReactorTestServiceGrpc
import org.horiga.study.armeria.grpc.v1.Service.SelectRequest
import org.horiga.study.armeria.grpc.v1.Service.SelectResponse
import org.horiga.study.armeria.repository.TestR2dbcRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import java.time.Duration

@Service
class TestService(
    val r2dbcRepository: TestR2dbcRepository
) : ReactorTestServiceGrpc.TestServiceImplBase() {

    override fun select(
        request: Mono<SelectRequest>
    ): Mono<SelectResponse> = request
        .handle { r, sink: SynchronousSink<SelectRequest> ->
            if (r.type != Message.MessageTypes.GENERAL && r.type != Message.MessageTypes.NORMAL)
                sink.error(
                    INVALID_ARGUMENT.withDescription("'type' parameter ignored")
                        .asRuntimeException()
                )
            else sink.next(r)
        }
        .flatMap { r ->
            r2dbcRepository.findByTypes(r.type.name.toLowerCase())
                .timeout(Duration.ofMillis(3000))
                .switchIfEmpty(Flux.empty())
                .onErrorResume { err ->
                    val grpcErrorStatus = when (err) {
                        is IllegalArgumentException -> INVALID_ARGUMENT.withDescription("<test>")
                        else -> UNKNOWN.withDescription(err.message)
                    }
                    Mono.error(grpcErrorStatus.asRuntimeException())
                }
                .map { entity -> entity.toMessage() }
                .collectList()
                .map { messages ->
                    SelectResponse.newBuilder()
                        .setFilterType(r.type)
                        .addAllItems(messages)
                        .build()
                }
        } // request.flatMap
}