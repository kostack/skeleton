package io.kostack.skeleton.security

import io.github.kostack.jwt_security.dto.JwtUser
import io.github.kostack.jwt_security.service.UserProvider
import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.service.UserService
import org.springframework.stereotype.Service

@Service
class UserProviderImpl(
  private val userService: UserService
) : UserProvider {
  override suspend fun load(
    identity: String,
    claims: Map<String, Any>
  ): JwtUser? {
    val roles =
      (claims["roles"] as? List<*>)
        ?.map { it as? String ?: error("Invalid role: expected a string") }
        ?: error("Missing or invalid roles claim")

    return userService.findByIdentity(identity)?.copy(roles = roles) ?: User(
      identity = identity,
      roles = roles
    )
  }
}
