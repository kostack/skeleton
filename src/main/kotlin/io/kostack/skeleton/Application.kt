package io.kostack.skeleton

import io.kostack.skeleton.configuration.AppProperties
import io.kostack.skeleton.configuration.CorsProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@EnableConfigurationProperties(AppProperties::class, CorsProperties::class)
@SpringBootApplication
class Application

fun main(args: Array<String>) {
  runApplication<Application>(*args)
}
