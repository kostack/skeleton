package io.kostack.skeleton.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer

@Configuration
class WebFluxConfiguration(
  private val corsProperties: CorsProperties
) : WebFluxConfigurer {
  override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
    configurer.addCustomResolver(ReactivePageableHandlerMethodArgumentResolver())
  }

  @Bean
  fun corsConfigurationSource(): CorsConfigurationSource {
    val config =
      CorsConfiguration().apply {
        allowedOriginPatterns = corsProperties.allowedOriginPatterns
        allowedMethods = corsProperties.allowedMethods
        allowedHeaders = corsProperties.allowedHeaders
        allowCredentials = corsProperties.allowCredentials
      }

    val source = UrlBasedCorsConfigurationSource()
    source.registerCorsConfiguration(corsProperties.pathPattern, config)
    return source
  }
}
