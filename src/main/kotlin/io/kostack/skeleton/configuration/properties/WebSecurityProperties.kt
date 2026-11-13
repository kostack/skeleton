package io.kostack.skeleton.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("web-security")
data class WebSecurityProperties(
  var basicAuthUser: String = "",
  var basicAuthPassword: String = ""
)
