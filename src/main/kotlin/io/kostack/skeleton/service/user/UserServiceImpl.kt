package io.kostack.skeleton.service.user

import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserServiceImpl(
  private val userRepository: UserRepository
) : UserService {
  override suspend fun save(user: User): User = userRepository.save(user)

  override suspend fun findByIdentity(identity: String): User? = userRepository.findUserByIdentity(identity)
}
