package io.kostack.skeleton.security

import io.kostack.skeleton.db.r2dbc.model.User
import org.springframework.data.domain.ReactiveAuditorAware
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class SecurityAuditorAware : ReactiveAuditorAware<String> {
  override fun getCurrentAuditor(): Mono<String> =
    ReactiveSecurityContextHolder
      .getContext()
      .map { ctx -> ctx.authentication ?: error("Authentication is null") }
      .map { auth: Authentication -> (auth.principal as User).identity }
}
