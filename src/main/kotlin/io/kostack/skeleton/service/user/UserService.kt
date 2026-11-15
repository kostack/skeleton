package io.kostack.skeleton.service.user

import io.kostack.skeleton.db.mongodb.model.User

interface UserService {
  suspend fun save(user: User): User
}
