package io.kostack.skeleton.configuration

import io.kostack.skeleton.configuration.properties.CorsProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
internal class CorsConfig(
  private val corsProperties: CorsProperties
) {
  @Bean
  fun corsConfigurationSource(): CorsConfigurationSource {
    val config =
      CorsConfiguration().apply {
        allowedOriginPatterns = corsProperties.allowedOriginPatterns
        allowedMethods = corsProperties.allowedMethods
        allowedHeaders = corsProperties.allowedHeaders
        allowCredentials = corsProperties.allowCredentials
      }

    return UrlBasedCorsConfigurationSource().apply {
      registerCorsConfiguration(corsProperties.pathPattern, config)
    }
  }
}
