package io.kostack.skeleton.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("web-security")
data class WebSecurityProperties(
  var basicAuthUser: String = "staging",
  var basicAuthPassword: String = "",
  var probeHeader: String = "x-k8s",
  var probeValue: String = ""
)
