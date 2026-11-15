package io.kostack.skeleton

import io.kostack.skeleton.configuration.properties.CorsProperties
import io.kostack.skeleton.configuration.properties.WebSecurityProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import java.time.ZoneOffset
import java.util.TimeZone

@EnableConfigurationProperties(
  WebSecurityProperties::class,
  CorsProperties::class
)
@SpringBootApplication
class Application

fun main(args: Array<String>) {
  TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC))
  runApplication<Application>(*args)
}
