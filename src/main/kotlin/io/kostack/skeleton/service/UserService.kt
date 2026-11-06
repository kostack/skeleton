package io.kostack.skeleton.service

import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.db.r2dbc.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
  private val userRepository: UserRepository
) {
  suspend fun save(user: User): User = userRepository.save(user)

  suspend fun findByIdentity(identity: String): User? = userRepository.findUserByIdentity(identity)
}
