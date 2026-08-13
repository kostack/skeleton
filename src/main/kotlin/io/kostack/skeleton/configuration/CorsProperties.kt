package io.kostack.skeleton.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
  val pathPattern: String,
  val allowedOriginPatterns: List<String>,
  val allowedMethods: List<String>,
  val allowedHeaders: List<String>,
  val allowCredentials: Boolean
)
