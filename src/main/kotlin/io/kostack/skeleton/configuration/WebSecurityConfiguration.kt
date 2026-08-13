package io.kostack.skeleton.configuration

import io.github.kostack.jwt_security.authentication.AuthEntryPointJwt
import io.github.kostack.jwt_security.authentication.JwtServerAuthenticationConverter
import io.github.kostack.jwt_security.authentication.TokenAuthenticationManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class WebSecurityConfiguration(
  val tokenAuthenticationManager: TokenAuthenticationManager,
  val jwtAuthenticationConverter: JwtServerAuthenticationConverter,
  val authEntryPointJwt: AuthEntryPointJwt
) {
  companion object {
    const val BASIC_AUTH_USER: String = "staging"
    const val BASIC_AUTH_PW: String = "z3xafXjfgUpN"

    private const val PROBE_HEADER: String = "x-k8s"
    private const val PROBE_VALUE: String = "secret"

    private val SWAGGER_RESOURCES =
      arrayOf(
        "/docs/**",
        "/swagger-ui/**",
        "/v3/api-docs/**"
      )

    private val API_RESOURCES =
      arrayOf(
        "/api/**"
      )
  }

  @Order(0)
  @Bean
  @Profile("dev", "staging")
  fun swaggerSecurity(http: ServerHttpSecurity): SecurityWebFilterChain {
    val encoder: PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    val userDetailsService =
      MapReactiveUserDetailsService(
        User
          .withUsername(BASIC_AUTH_USER)
          .password(encoder.encode(BASIC_AUTH_PW))
          .roles("SWAGGER")
          .build()
      )

    val authManager = UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService)

    return http
      .securityMatcher(ServerWebExchangeMatchers.pathMatchers(*SWAGGER_RESOURCES))
      .csrf { it.disable() }
      .formLogin { it.disable() }
      .httpBasic { httpBasic ->
        httpBasic.authenticationManager(authManager)
      }.authorizeExchange { exchanges ->
        exchanges.anyExchange().hasRole("SWAGGER")
      }.build()
  }

  @Order(1)
  @Bean
  fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
    val authenticationWebFilter =
      AuthenticationWebFilter(tokenAuthenticationManager).apply {
        setServerAuthenticationConverter(jwtAuthenticationConverter)
      }

    return http
      .exceptionHandling { it.authenticationEntryPoint(authEntryPointJwt) }
      .csrf { it.disable() }
      .formLogin { it.disable() }
      .httpBasic { it.disable() }
      .logout { it.disable() }
      .authorizeExchange { exchanges ->
        exchanges
          .pathMatchers("/actuator/**")
          .access { _, context ->
            val header =
              context.exchange.request.headers
                .getFirst(PROBE_HEADER)
            val allowed = PROBE_VALUE == header
            Mono.just(AuthorizationDecision(allowed))
          }.pathMatchers(*API_RESOURCES)
          .authenticated()
          .anyExchange()
          .authenticated()
      }.addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
      .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
      .build()
  }
}
