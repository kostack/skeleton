package io.kostack.skeleton.security

import io.github.kostack.jwt_security.dto.JwtUser
import io.github.kostack.jwt_security.extensions.getClaim
import io.github.kostack.jwt_security.service.UserProvider
import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.service.user.UserService
import org.springframework.stereotype.Service

@Service
class UserProviderImpl(
  private val userService: UserService
) : UserProvider {
  override suspend fun load(
    identity: String,
    claims: Map<String, Any>
  ): JwtUser? =
    userService
      .findByIdentity(identity) ?: User(
      identity = identity,
      roles = claims.getClaim<List<String>>("roles")
    )
}
