package io.kostack.skeleton.integration.api.todo.operation

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.api.todo.response.TodoListResponse
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
class ListTodoIntegrationTest : BaseIntegrationTest() {
  private fun todo(reference: Int): Todo = references.getReference<Todo>("todo_$reference")

  @Test
  fun `list returns only the current user's todos`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/todos")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Pagination<TodoListResponse>>()
        .value { response ->
          assertEquals(3, response?.total)
          assertEquals(
            setOf(todo(1).identifier, todo(2).identifier, todo(3).identifier),
            response?.content?.map { it.identifier }?.toSet()
          )
        }
    }

  @Test
  fun `list filters by completed`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri { it.path("/api/v1/todos").queryParam("completed", "true").build() }
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Pagination<TodoListResponse>>()
        .value { response ->
          assertEquals(listOf(todo(3).identifier), response?.content?.map { it.identifier })
        }
    }

  @Test
  fun `list with invalid query returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri { it.path("/api/v1/todos").queryParam("title", "a").build() }
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

  @Test
  fun `missing token is rejected`() {
    webTestClient
      .get()
      .uri("/api/v1/todos")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
