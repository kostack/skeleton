package io.kostack.skeleton.service

import io.github.kostack.jwt_security.extensions.getClaim
import io.github.kostack.jwt_security.service.JwtUser
import io.github.kostack.jwt_security.service.JwtUserService
import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.db.r2dbc.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
  private val userRepository: UserRepository
) : JwtUserService {
  suspend fun save(user: User): User = userRepository.save(user)

  override suspend fun findByIdentity(identity: String): JwtUser? =
    userRepository.findUserByIdentity(identity) as? JwtUser

  override suspend fun upsertFromPayload(payload: Map<String, Any>): JwtUser {
    val identity = payload.getClaim<String>("sub")
    val payloadUsername = payload.getClaim<String>("username")
    val payloadRoles = payload.getClaim<List<String>>("roles")

    val user =
      userRepository.findUserByIdentity(identity)?.apply {
        username = payloadUsername
        roles = payloadRoles
      } ?: User(
        username = payloadUsername,
        identity = identity,
        roles = payloadRoles
      )

    return user as JwtUser
  }
}
