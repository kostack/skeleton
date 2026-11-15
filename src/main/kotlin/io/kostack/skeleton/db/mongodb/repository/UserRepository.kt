package io.kostack.skeleton.db.mongodb.repository

import io.kostack.skeleton.db.mongodb.model.User
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CoroutineCrudRepository<User, Long> {
  suspend fun findUserByIdentity(identity: String): User?
}
