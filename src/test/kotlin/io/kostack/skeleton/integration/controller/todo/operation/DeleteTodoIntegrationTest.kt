package io.kostack.skeleton.integration.controller.todo.operation

import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Fixtures:
 * - user_1 owns todo_1, todo_2 (open) and todo_3 (completed)
 * - user_2 owns todo_4 (open) and todo_5 (completed)
 * - project_1 holds todo_1, todo_2 and todo_4; project_2 holds todo_3 and todo_5
 */
class DeleteTodoIntegrationTest : BaseIntegrationTest() {
  @Autowired
  private lateinit var todoRepository: TodoRepository

  private fun todo(reference: Int): Todo = references.getReference<Todo>("todo_$reference")

  @Test
  fun `delete removes the todo`() =
    runTest {
      val todo = todo(2)

      clientFor(user("user-1"))
        .delete()
        .uri("/api/v1/todos/{todoId}", todo.identifier)
        .exchange()
        .expectStatus()
        .isNoContent
        .expectBody()
        .isEmpty

      assertNull(todoRepository.findByIdentifier(todo.identifier))
    }

  @Test
  fun `delete unknown todo returns 404`() =
    runTest {
      clientFor(user("user-1"))
        .delete()
        .uri("/api/v1/todos/{todoId}", "does-not-exist")
        .exchange()
        .expectStatus()
        .isNotFound
    }

  @Test
  fun `delete another user's todo returns 404 and keeps it`() =
    runTest {
      val todo = todo(4)

      clientFor(user("user-1"))
        .delete()
        .uri("/api/v1/todos/{todoId}", todo.identifier)
        .exchange()
        .expectStatus()
        .isNotFound

      assertNotNull(todoRepository.findByIdentifier(todo.identifier))
    }
}
