package io.kostack.skeleton.configuration

import io.github.kostack.jwt_security.converter.HeaderOrQueryTokenAuthenticationConverter
import io.github.kostack.jwt_security.converter.JwtAuthenticationConverter
import io.github.kostack.jwt_security.service.AuthEntryPointJwt
import io.kostack.skeleton.configuration.properties.WebSecurityProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class WebSecurityConfig(
  val webSecurityProperties: WebSecurityProperties
) {
  companion object {
    private val API_DOCS_RESOURCES =
      arrayOf(
        "/docs/**",
        "/v3/api-docs/**"
      )

    private val HEALTH_RESOURCES =
      arrayOf(
        "/actuator/health",
        "/actuator/health/**"
      )

    private val API_RESOURCES =
      arrayOf(
        "/api/**"
      )
  }

  @Order(0)
  @Bean
  @Profile("dev", "staging")
  fun apiDocsSecurity(http: ServerHttpSecurity): SecurityWebFilterChain {
    check(webSecurityProperties.basicAuthUser.isNotBlank() && webSecurityProperties.basicAuthPassword.isNotBlank()) {
      "web-security.basic-auth-user and web-security.basic-auth-password must be set (BASIC_AUTH_USER, BASIC_AUTH_PASSWORD)"
    }

    val encoder: PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    val userDetailsService =
      MapReactiveUserDetailsService(
        User
          .withUsername(webSecurityProperties.basicAuthUser)
          .password(encoder.encode(webSecurityProperties.basicAuthPassword))
          .roles("API_DOCS")
          .build()
      )

    val authManager = UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService)

    return http
      .securityMatcher(ServerWebExchangeMatchers.pathMatchers(*API_DOCS_RESOURCES))
      .csrf { it.disable() }
      .formLogin { it.disable() }
      .httpBasic { httpBasic ->
        httpBasic.authenticationManager(authManager)
      }.authorizeExchange { exchanges ->
        exchanges.anyExchange().hasRole("API_DOCS")
      }.build()
  }

  @Order(1)
  @Bean
  fun securityWebFilterChain(
    converter: JwtAuthenticationConverter,
    authEntryPointJwt: AuthEntryPointJwt,
    http: ServerHttpSecurity,
    tokenConverter: HeaderOrQueryTokenAuthenticationConverter
  ): SecurityWebFilterChain =
    http
      .exceptionHandling { it.authenticationEntryPoint(authEntryPointJwt) }
      .csrf { it.disable() }
      .formLogin { it.disable() }
      .httpBasic { it.disable() }
      .logout { it.disable() }
      .authorizeExchange { exchanges ->
        exchanges
          .pathMatchers(*HEALTH_RESOURCES)
          .permitAll()
          .pathMatchers(*API_RESOURCES)
          .authenticated()
          .anyExchange()
          .authenticated()
      }.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
      .oauth2ResourceServer {
        it.bearerTokenConverter(tokenConverter)
        it.authenticationEntryPoint(authEntryPointJwt)
        it.jwt { jwt -> jwt.jwtAuthenticationConverter(converter) }
      }.build()
}
