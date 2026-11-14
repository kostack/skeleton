package io.kostack.skeleton.cloud.stream

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.function.Consumer

@Configuration
class DemoStream {
  @Profile("cloud-consumer")
  @Bean
  fun demo(): Consumer<String> =
    Consumer { msg ->
      log.info("Received message: {}", msg)
    }

  companion object {
    val log: Logger = LoggerFactory.getLogger(DemoStream::class.java)
  }
}
