package io.kostack.skeleton.integration.controller.todo.operation

import io.kostack.skeleton.controller.todo.response.TodoResponse
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals

/**
 * Fixtures:
 * - user_1 owns todo_1, todo_2 (open) and todo_3 (completed)
 * - user_2 owns todo_4 (open) and todo_5 (completed)
 * - project_1 holds todo_1, todo_2 and todo_4; project_2 holds todo_3 and todo_5
 */
class GetTodoIntegrationTest : BaseIntegrationTest() {
  private fun todo(reference: Int): Todo = references.getReference<Todo>("todo_$reference")

  @Test
  fun `get returns the todo`() =
    runTest {
      val todo = todo(1)

      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/todos/{todoId}", todo.identifier)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<TodoResponse>()
        .value { response ->
          assertEquals(todo.identifier, response?.identifier)
        }
    }

  @Test
  fun `get unknown todo returns 404`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/todos/{todoId}", "does-not-exist")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound
    }

  @Test
  fun `get another user's todo returns 404`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/todos/{todoId}", todo(4).identifier)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound
    }
}
