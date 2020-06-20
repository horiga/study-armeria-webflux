study-armeria-webflux
=====

example project that gRPC server with [Armeria](https://github.com/line/armeria) and spring-boot.

### How to run
- build proto

```bash
$ ./gradlew generateProro
```

- start server

```bash
$ ./gradlew bootRun
```

```
WARN 65730 --- [           main] .a.s.w.r.ArmeriaReactiveWebServerFactory : Cannot disable HTTP/2 protocol for Armeria server. It will be enabled automatically.
INFO 65730 --- [           main] c.l.a.server.docs.DocStringExtractor     : Using com.linecorp.armeria.grpc.descriptorDir: META-INF/armeria/grpc
INFO 65730 --- [           main] c.l.armeria.common.util.SystemInfo       : Hostname: horiga-imac.local (from 'hostname' command)
INFO 65730 --- [entExecutor-3-1] c.l.a.i.shaded.reflections.Reflections   : Reflections took 3 ms to scan 1 urls, producing 1 keys and 1 values
INFO 65730 --- [oss-http-*:8080] com.linecorp.armeria.server.Server       : Serving HTTP at /0:0:0:0:0:0:0:0:8080 - http://127.0.0.1:8080/
INFO 65730 --- [           main] org.horiga.study.armeria.ApplicationKt   : Started ApplicationKt in 1.818 seconds (JVM running for 2.165)
INFO 65730 --- [-worker-nio-2-1] c.l.a.i.common.JavaVersionSpecific       : Using the APIs optimized for: Java 9+

```

- execute grpc

> grpcurl - https://github.com/fullstorydev/grpcurl

```bash
$ grpcurl -plaintext -d '{"message":"horiga"}' 127.0.0.1:8080 org.horiga.study.armeria.grpc.v1.HelloService/SayHello
  {
    "message": "Hello, horiga"
  }
```

- start MySQL with docker

```bash
$ cd docker
$ docker-compose up
```

> spring r2dbc properties
> ```$xslt
> spring.r2dbc.url=r2dbc:pool:mysql://127.0.0.1:3306/demo
> spring.r2dbc.username=test
> spring.r2dbc.password=test
> spring.r2dbc.pool.initial-size=10
> spring.r2dbc.pool.max-size=50
> spring.r2dbc.pool.max-idle-time=30s
> spring.r2dbc.pool.validation-query=SELECT 1
> ```

- execute grpc

```bash
$ grpcurl -plaintext -d '{"type":"GENERAL"}' 127.0.0.1:8080 org.horiga.study.armeria.grpc.v1.R2dbcService/Select
{
  "filter_type": "general",
  "items": [
    {
      "id": "1",
      "name": "foo",
      "type": "general"
    },
    {
      "id": "2",
      "name": "bar",
      "type": "general"
    }
  ]
}
```

### Kotlin

Write with Kotlin and Reactor

```Kotlin
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
```

```Kotlin
@Service
class R2dbcService(
        val r2dbcRepository: TestR2dbcRepository
) : ReactorR2dbcServiceGrpc.R2dbcServiceImplBase() {

    companion object {
        val availableMessageTypes = setOf(
                MessageTypes.GENERAL,
                MessageTypes.NORMAL,
                MessageTypes.URGENT
        )
    }

    override fun select(
            request: Mono<SelectRequest>
    ): Mono<SelectResponse> = request
            .handle { r, sink: SynchronousSink<SelectRequest> ->
                if (!availableMessageTypes.contains(r.type))
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
```
