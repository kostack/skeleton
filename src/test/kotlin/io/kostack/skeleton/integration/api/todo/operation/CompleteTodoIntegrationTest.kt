package io.kostack.skeleton.integration.api.todo.operation

import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.returnResult
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Fixtures:
 * - user_1 owns todo_1, todo_2 (open) and todo_3 (completed)
 * - user_2 owns todo_4 (open) and todo_5 (completed)
 * - project_1 holds todo_1, todo_2 and todo_4; project_2 holds todo_3 and todo_5
 */
class CompleteTodoIntegrationTest : BaseIntegrationTest() {
  @Autowired
  private lateinit var todoRepository: TodoRepository

  private fun todo(reference: Int): Todo = references.getReference<Todo>("todo_$reference")

  @Test
  fun `complete marks the todo as completed`() =
    runTest {
      val todo = todo(1)

      clientFor(user("user-1"))
        .patch()
        .uri("/api/v1/todos/{todoId}/complete", todo.identifier)
        .exchange()
        .expectStatus()
        .isNoContent
        .expectBody()
        .isEmpty

      val stored = todoRepository.findByIdentifier(todo.identifier)
      assertEquals(true, stored?.completed)
      assertEquals(todo.createdAt, stored?.createdAt)
      assertTrue(stored!!.lastModifiedAt > todo.lastModifiedAt, "lastModifiedAt was not updated")
    }

  @Test
  fun `complete unknown todo returns 404`() =
    runTest {
      clientFor(user("user-1"))
        .patch()
        .uri("/api/v1/todos/{todoId}/complete", "does-not-exist")
        .exchange()
        .expectStatus()
        .isNotFound
    }

  @Test
  fun `complete another user's todo returns 404 and leaves it unchanged`() =
    runTest {
      val todo = todo(4)

      clientFor(user("user-1"))
        .patch()
        .uri("/api/v1/todos/{todoId}/complete", todo.identifier)
        .exchange()
        .expectStatus()
        .isNotFound

      assertFalse(todoRepository.findByIdentifier(todo.identifier)!!.completed)
    }

  @Test
  fun `complete already completed todo returns 204 and leaves it unchanged`() =
    runTest {
      val todo = todo(3)

      clientFor(user("user-1"))
        .patch()
        .uri("/api/v1/todos/{todoId}/complete", todo.identifier)
        .exchange()
        .expectStatus()
        .isNoContent

      val stored = todoRepository.findByIdentifier(todo.identifier)
      assertEquals(true, stored?.completed)
      assertEquals(todo.version, stored?.version)
      assertEquals(todo.lastModifiedAt, stored?.lastModifiedAt)
    }

  @Test
  fun `complete without token returns 401 and leaves the todo unchanged`() =
    runTest {
      val todo = todo(1)

      webTestClient
        .patch()
        .uri("/api/v1/todos/{todoId}/complete", todo.identifier)
        .exchange()
        .expectStatus()
        .isUnauthorized

      assertFalse(todoRepository.findByIdentifier(todo.identifier)!!.completed)
    }

  @Test
  fun `concurrent completes all succeed and complete the todo once`() =
    runTest {
      val todo = todo(1)
      val client = clientFor(user("user-1"))

      val statuses =
        concurrently(CONCURRENT_REQUESTS) {
          client
            .patch()
            .uri("/api/v1/todos/{todoId}/complete", todo.identifier)
            .exchange()
            .returnResult<Void>()
            .status
        }

      assertEquals(List(CONCURRENT_REQUESTS) { HttpStatus.NO_CONTENT }, statuses)
      val stored = todoRepository.findByIdentifier(todo.identifier)
      assertEquals(true, stored?.completed)
      assertEquals(todo.version!! + 1, stored?.version)
    }

  companion object {
    private const val CONCURRENT_REQUESTS = 8
  }
}
