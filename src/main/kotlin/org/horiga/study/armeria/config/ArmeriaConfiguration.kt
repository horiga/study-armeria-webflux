/*
 * Copyright (c) 2020 LINE Corporation. All rights reserved.
 * LINE Corporation PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.horiga.study.armeria.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.linecorp.armeria.server.docs.DocService
import com.linecorp.armeria.server.grpc.GrpcService
import com.linecorp.armeria.server.logging.AccessLogWriter
import com.linecorp.armeria.server.logging.LoggingService
import com.linecorp.armeria.spring.ArmeriaServerConfigurator
import io.grpc.BindableService
import io.grpc.protobuf.services.ProtoReflectionService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ArmeriaConfiguration {

    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.INDENT_OUTPUT, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
        .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    /**
     * @see https://line.github.io/armeria/advanced-spring-webflux-integration.html
     */
    @Bean
    fun armeriaServerConfigurator(bindableServices: List<BindableService>): ArmeriaServerConfigurator =
        ArmeriaServerConfigurator { sb ->
            sb.accessLogWriter(AccessLogWriter.combined(), false)
            sb.serviceUnder("/docs", DocService())
            sb.decorator(LoggingService.newDecorator())
            sb.service(GrpcService.builder()
                    .addServices(bindableServices)
                    .addService(ProtoReflectionService.newInstance())
                    .enableUnframedRequests(true)
                    .build()
            )
        }
}