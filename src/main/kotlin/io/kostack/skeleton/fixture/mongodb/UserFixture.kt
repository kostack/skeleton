package io.kostack.skeleton.fixture.mongodb

import io.github.kostack.fixtures.AbstractFixture
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
class UserFixture(
  private val userRepository: UserRepository
) : AbstractFixture() {
  override suspend fun load() {
    createUser(1, "Alice Example")
    createUser(2, "Bob Example")
    createUser(3, "Carol Example")
  }

  suspend fun createUser(
    idx: Int,
    name: String
  ) {
    val user =
      User(identity = "user-$idx", name = name, email = "user-$idx@example.com", roles = listOf("ROLE_USER"))
    this.referenceRepository.setReference("user_$idx", userRepository.save(user))
  }
}
