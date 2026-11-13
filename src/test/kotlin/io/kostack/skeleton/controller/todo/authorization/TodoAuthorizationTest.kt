package io.kostack.skeleton.controller.todo.authorization

import io.kostack.skeleton.db.mongodb.model.Todo
import io.kostack.skeleton.db.r2dbc.model.User
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import java.time.Duration

class TodoAuthorizationTest {
  private val authorization = TodoAuthorization()

  private val owner = User(identity = "user-1", roles = listOf("ROLE_USER"))
  private val otherUser = User(identity = "user-2", roles = listOf("ROLE_USER"))
  private val todo = Todo(id = "todo-1", title = "Write unit tests", createdBy = owner.identity)

  @Test
  fun `requireOwner grants access to the user who created the todo`() {
    StepVerifier
      .create(authorization.requireOwner(todo, owner))
      .expectNext(true)
      .expectComplete()
      .verify(TIMEOUT)
  }

  @Test
  fun `requireOwner denies access to a user who did not create the todo`() {
    StepVerifier
      .create(authorization.requireOwner(todo, otherUser))
      .expectNext(false)
      .expectComplete()
      .verify(TIMEOUT)
  }

  @Test
  fun `requireOwner denies access to an admin who did not create the todo`() {
    val admin = User(identity = "admin-1", roles = listOf("ROLE_ADMIN"))

    StepVerifier
      .create(authorization.requireOwner(todo, admin))
      .expectNext(false)
      .expectComplete()
      .verify(TIMEOUT)
  }

  companion object {
    private val TIMEOUT = Duration.ofSeconds(5)
  }
}
