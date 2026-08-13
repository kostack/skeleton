package io.kostack.skeleton.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration
class JacksonConfig {
  @Bean
  fun objectMapper(): ObjectMapper = ObjectMapper()
}
