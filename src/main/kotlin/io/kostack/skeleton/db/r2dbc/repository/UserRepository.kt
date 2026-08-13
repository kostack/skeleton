package io.kostack.skeleton.db.r2dbc.repository

import io.kostack.skeleton.db.r2dbc.model.User
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CoroutineCrudRepository<User, Long> {
  suspend fun findUserByIdentity(identity: String): User?
}
