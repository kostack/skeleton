package io.kostack.skeleton.security.authorization

import io.kostack.skeleton.db.r2dbc.model.User
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component("defaultAuth")
class DefaultAuthorization {
  fun canView(user: Any?): Mono<Boolean> =
    mono {
      (user as? User)?.hasRole("ROLE_ADMIN") == true
    }
}
