package io.kostack.skeleton

import io.kostack.skeleton.configuration.CorsProperties
import io.kostack.skeleton.configuration.properties.WebSecurityProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@EnableConfigurationProperties(
  WebSecurityProperties::class,
  CorsProperties::class
)
@SpringBootApplication
class Application

fun main(args: Array<String>) {
  runApplication<Application>(*args)
}
