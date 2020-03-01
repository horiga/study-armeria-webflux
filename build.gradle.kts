import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.google.protobuf.gradle.*

plugins {
	id("idea")
	id("java")

	id("org.springframework.boot") version "2.2.5.RELEASE"
	id("io.spring.dependency-management") version "1.0.9.RELEASE"
	id("com.google.protobuf") version "0.8.11"

	kotlin("jvm") version "1.3.61"
	kotlin("plugin.spring") version "1.3.61"

}

group = "org.horiga"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	google()
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {

	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	// Armeria
	implementation("com.linecorp.armeria:armeria-spring-boot-webflux-starter")
	implementation("com.linecorp.armeria:armeria-grpc")

	// spring-boot
	// implementation("org.springframework.boot:spring-boot-starter-webflux") - Note: armeria-spring-boot-webflux
	implementation("org.springframework.boot.experimental:spring-boot-starter-data-r2dbc")

	// mysql
	implementation("com.github.jasync-sql:jasync-r2dbc-mysql:1.0.14")
	implementation("io.r2dbc:r2dbc-pool:0.8.1.RELEASE")

	// Reactor
	implementation("io.projectreactor.addons:reactor-extra:3.3.2.RELEASE")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.0.2.RELEASE")
	implementation("com.salesforce.servicelibs:reactor-grpc-stub:1.0.0")

	// Misc
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	// Kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("org.springframework.boot.experimental:spring-boot-test-autoconfigure-r2dbc")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("io.mockk:mockk:1.9.3")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.boot.experimental:spring-boot-bom-r2dbc:0.1.0.M3")
		mavenBom("com.linecorp.armeria:armeria-bom:0.98.3")
		mavenBom("io.netty:netty-bom:4.1.45.Final")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "1.8"
	}
}

plugins.withType<ProtobufPlugin> {
	sourceSets {
		main {
			proto {
				srcDir("proto")
			}
		}
	}
	protobuf {
		protoc {
			artifact = "com.google.protobuf:protoc:3.10.1"
		}
		plugins {
			id("grpc") {
				artifact = "io.grpc:protoc-gen-grpc-java:1.25.0"
			}
			id("reactorGrpc") {
				artifact = "com.salesforce.servicelibs:reactor-grpc:1.0.0"
			}
		}
		generateProtoTasks {
			ofSourceSet("main").forEach {
				it.plugins {
					id("grpc")
					id("reactorGrpc")
				}
			}
		}
	}
}
