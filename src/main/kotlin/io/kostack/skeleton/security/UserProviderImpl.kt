package io.kostack.skeleton.security

import io.github.kostack.jwt_security.dto.JwtUser
import io.github.kostack.jwt_security.extensions.getClaim
import io.github.kostack.jwt_security.service.UserProvider
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserProviderImpl(
  private val userRepository: UserRepository
) : UserProvider {
  override suspend fun load(
    identity: String,
    claims: Map<String, Any>
  ): JwtUser? =
    userRepository
      .findByIdentity(identity) ?: User(
      identity = identity,
      name = claims.getClaim<String>("name"),
      email = claims.getClaim<String>("email"),
      roles = claims.getClaim<List<String>>("roles")
    )
}
