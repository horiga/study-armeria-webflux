/*
 * Copyright (c) 2020 LINE Corporation. All rights reserved.
 * LINE Corporation PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.horiga.study.armeria.repository

import org.horiga.study.armeria.grpc.v1.Message
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Table("test")
data class Test(
    @Id
    var id: String,
    @Column("name")
    var name: String,
    @Column("type")
    var type: String
) {
    fun toMessage() = Message.TestMessage.newBuilder()
            .setId(this.id)
            .setName(this.name)
            .setType(Message.MessageTypes.valueOf(this.name.toUpperCase()))
            .build()
}

@Repository
interface TestR2dbcRepository : R2dbcRepository<Test, String> {

    @Query("""
       SELECT id, name, type FROM test WHERE type = :type
    """)
    fun findByTypes(@Param("type") type: String): Flux<Test>
}