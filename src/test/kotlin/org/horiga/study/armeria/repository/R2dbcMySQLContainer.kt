package org.horiga.study.armeria.repository

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.annotation.AliasFor
import org.springframework.core.env.Environment
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.lang.annotation.Inherited
import java.nio.file.Paths
import kotlin.reflect.KClass

@ConfigurationProperties(prefix = "test.docker")
data class R2dbcMySQLTestcontainersProperties(
    var dockerComposeFile: String = "docker/docker-compose.test.yml",
    var mysql: MySQLProperties = MySQLProperties()
) {
    companion object {
        fun fromEnvironment(env: Environment) = R2dbcMySQLTestcontainersProperties().apply {
            dockerComposeFile =
                env.getProperty("test.docker.docker-compose-file", "docker/docker-compose.test.yml")
            mysql.name = env.getProperty("test.docker.mysql.name", "test")
            mysql.port = env.getProperty("test.docker.mysql.port", "3306").toInt()
            mysql.serviceName = env.getProperty("test.docker.mysql.serviceName", "mysql")
        }
    }

    data class MySQLProperties(
        var name: String = "test",
        var port: Int = 3306,
        var serviceName: String = "mysql"
    )
}

@Suppress("unused")
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@EnableConfigurationProperties(R2dbcMySQLTestcontainersProperties::class)
@ContextConfiguration
annotation class WithR2dbcMySQLContainer(
    @get:AliasFor(annotation = ContextConfiguration::class, attribute = "initializers")
    @Suppress("unused")
    val initializers: Array<KClass<out ApplicationContextInitializer<*>>> =
        [R2dbcMySQLContainer.Initializer::class]
)

object R2dbcMySQLContainer {

    val log = LoggerFactory.getLogger(R2dbcMySQLContainer::class.java)!!

    private lateinit var properties: R2dbcMySQLTestcontainersProperties
    private val container: KDockerComposeContainer by lazy {
        KDockerComposeContainer(
            Paths.get(properties.dockerComposeFile).toAbsolutePath().normalize().toFile()
        )
            .withExposedService(
                properties.mysql.serviceName,
                properties.mysql.port,
                Wait.forLogMessage(".*ready for connections.*\\s", 2)
            )
            .withTailChildContainers(true)
    }

    fun getUrl() = "r2dbc:pool:mysql://${container.getServiceHost(
        properties.mysql.serviceName, properties.mysql.port
    )}:${container.getServicePort(
        properties.mysql.serviceName, properties.mysql.port
    )}/${properties.mysql.name}"

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            try {
                properties =
                    R2dbcMySQLTestcontainersProperties.fromEnvironment(
                        applicationContext.environment
                    )
                container.start()

                val connectUrl = getUrl()

                log.info("spring.r2dbc.url=$connectUrl")

                TestPropertyValues.of(
                    "spring.r2dbc.url=$connectUrl"
                ).applyTo(applicationContext)

                Runtime.getRuntime().addShutdownHook(Thread {
                    container.stop()
                })
            } catch (e: Exception) {
                container.stop()
                throw IllegalStateException("Docker initialization failed", e)
            }
        }
    }

    private class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)
}