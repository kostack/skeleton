package io.kostack.skeleton.fixture.r2dbc

import io.github.kostack.fixtures.AbstractFixture
import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.service.UserService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
class UserFixture(
  val userService: UserService
) : AbstractFixture() {
  override suspend fun load() {
    fixture1()
  }

  suspend fun fixture1() {
    val user = User(identity = "admin-1", roles = listOf("ROLE_ADMIN"))
    this.referenceRepository.setReference("user_1", user)
    userService.save(user)
  }
}
