package io.kostack.skeleton.fixture.r2dbc

import io.github.kostack.fixtures.AbstractFixture
import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.service.user.UserService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
class UserFixture(
  val userService: UserService
) : AbstractFixture() {
  override suspend fun load() {
    fixture1()
    fixture2()
  }

  suspend fun fixture1() {
    val user = User(identity = "user-1", roles = listOf("ROLE_USER"))
    this.referenceRepository.setReference("user_1", userService.save(user))
  }

  suspend fun fixture2() {
    val user = User(identity = "user-2", roles = listOf("ROLE_USER"))
    this.referenceRepository.setReference("user_2", userService.save(user))
  }
}
