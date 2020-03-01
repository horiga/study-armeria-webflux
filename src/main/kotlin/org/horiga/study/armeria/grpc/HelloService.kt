/*
 * Copyright (c) 2020 LINE Corporation. All rights reserved.
 * LINE Corporation PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.horiga.study.armeria.grpc

import org.horiga.study.armeria.grpc.v1.ReactorHelloServiceGrpc
import org.horiga.study.armeria.grpc.v1.Service.HelloRequest
import org.horiga.study.armeria.grpc.v1.Service.HelloResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class HelloService: ReactorHelloServiceGrpc.HelloServiceImplBase() {

    companion object {
        val log = LoggerFactory.getLogger(HelloService::class.java)!!
    }

    override fun sayHello(
        request: Mono<HelloRequest>
    ): Mono<HelloResponse> = request.flatMap { thisRequest ->
        log.info("Handle RPC Message: sayHello(${thisRequest.message})")
        Mono.just(HelloResponse.newBuilder().setMessage("Hello, ${thisRequest.message}").build())
    }
}