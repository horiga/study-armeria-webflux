study-armeria-webflux
=====

[Armeria](https://github.com/line/armeria) と spring-boot を使ったgRPCサーバのサンプルコード


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

- Call RPC

```bash
$ grpcurl -plaintext -d '{"message":"horiga"}' 127.0.0.1:8080 org.horiga.study.armeria.grpc.v1.HelloService/SayHello
  {
    "message": "Hello, horiga"
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