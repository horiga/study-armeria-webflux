/*
 * Copyright (c) 2020 LINE Corporation. All rights reserved.
 * LINE Corporation PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.horiga.study.armeria.grpc

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

    override fun select(
        request: Mono<SelectRequest>
    ): Mono<SelectResponse> = request.flatMap { thisRequest ->
        r2dbcRepository.findByTypes(thisRequest.type)
            .timeout(Duration.ofMillis(3000))
            .doOnError { err -> log.error("failed to select from test table. type=${thisRequest.type}", err) }
            .onErrorResume { err ->
                log.warn("onErrorResume, r2dbc, findByTypes", err)
                Flux.empty()
            }
            .map { it.toMessage() }
            .collectList()
            .map { items ->
                SelectResponse.newBuilder()
                    .setFilterType(thisRequest.type)
                    .addAllItems(items)
                    .build()
            }
    }
}