package io.kostack.skeleton.service.user

import io.kostack.skeleton.db.r2dbc.model.User

interface UserService {
  suspend fun save(user: User): User

  suspend fun findByIdentity(identity: String): User?
}
