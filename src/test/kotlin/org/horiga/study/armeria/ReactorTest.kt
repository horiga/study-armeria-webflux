package org.horiga.study.armeria

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ReactorTest {

    companion object {
        val log = LoggerFactory.getLogger(ReactorTest::class.java)!!
    }

    class ReactorTestException(message: String, cause: Throwable? = null) : Exception(message, cause)

    data class Profile(val id: String, val name: String, var validated: Boolean = false)

    class DemoRepository {
        fun getIds(n: Int = 0, prefix: String = ""): Flux<String> = when {
            n < 0 -> Flux.error(ReactorTestException("error"))
            n == 0 -> Flux.empty()
            else -> Flux.fromArray(IntRange(1, n).map { prefix + UUID.randomUUID().toString() }.toTypedArray())
        }

        fun getProfile(id: String? = ""): Mono<Profile> = when(id) {
            null -> Mono.error(ReactorTestException("error"))
            "" -> Mono.empty()
            else -> Profile(id, "Reactor test").toMono()
        }
    }

    class DemoService {

        val repository = DemoRepository()

        fun testFlux(n: Int = 10): Flux<String> = repository.getIds(n)
            .delayElements(Duration.ofMillis(300))
            .doOnError { err -> log.warn("(doOnError)", err) }
            .map { it.toUpperCase() }

        fun testMono(id: String? = ""): Mono<Profile> = repository.getProfile(id)
            .doOnError { err -> log.warn("(doOnError)", err) }
            .map {
                it.validated = true
                it
            }
            .defaultIfEmpty(Profile("<undefined>", "EMPTY USER"))

        fun testSubscribe(latch: CountDownLatch): Disposable {
            var counter = 0
            return repository.getIds(5)
                .delayElements(Duration.ofMillis(100))
                .map { it.toUpperCase() }
                .subscribe {
                    counter++
                    log.info(">> subscribed($counter): $it")
                    if (counter == 5) {
                        latch.countDown()
                    }
                }
        }
    }

    @Test
    fun test1() {
        StepVerifier.create(
                DemoService().testFlux()
            )
            .thenConsumeWhile {
                log.info("[thenConsumeWhile]: $it")
                true
            }
            .verifyComplete()

        StepVerifier.create(
            DemoService().testFlux(0)
        ).expectNextCount(0).verifyComplete()

        StepVerifier.create(
                DemoService().testFlux(-1)
            )
            .expectError(ReactorTestException::class.java)
            .verify()

        // -- Mono

        StepVerifier.create(
            DemoService().testMono(UUID.randomUUID().toString())
        ).consumeNextWith { p -> log.info("[consumeNextWith] $p") }
            .verifyComplete()

        StepVerifier.create(
            DemoService().testMono("") // empty
        ).consumeNextWith { p ->
            log.info("[consumeNextWith] $p")
            Assertions.assertThat(p.id).isEqualTo("<undefined>")
        }.verifyComplete()

        StepVerifier.create(
            DemoService().testMono(null) // exception
        ).expectErrorMatches { err ->
            log.info("[expectErrorMatches]", err)
            err is ReactorTestException
        }.verify()

        val latch = CountDownLatch(1)
        val disposable = DemoService().testSubscribe(latch)

        log.info("disposable.isDisposed=${disposable.isDisposed}")

        latch.await(5000, TimeUnit.MILLISECONDS)

        log.info("disposable.isDisposed=${disposable.isDisposed}")
    }
}