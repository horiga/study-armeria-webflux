package org.horiga.study.armeria

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StudyArmeriaWebfluxApplication

fun main(args: Array<String>) {
	runApplication<StudyArmeriaWebfluxApplication>(*args)
}
