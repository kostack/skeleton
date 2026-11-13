package io.kostack.skeleton.integration.controller

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.spring_tools.dto.ApiValidation
import io.kostack.skeleton.controller.todo.response.TodoListResponse
import io.kostack.skeleton.controller.todo.response.TodoResponse
import io.kostack.skeleton.db.mongodb.model.Todo
import io.kostack.skeleton.integration.BaseControllerIntegrationTest
import io.kostack.skeleton.service.todo.TodoService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Fixtures:
 * - user_1 owns todo_1, todo_2 (open) and todo_3 (completed)
 * - user_2 owns todo_4 (open) and todo_5 (completed)
 */
class TodoControllerTest : BaseControllerIntegrationTest() {
  @Autowired
  private lateinit var todoService: TodoService

  private fun todo(reference: Int): Todo = references.getReference<Todo>("todo_$reference")

  // list

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
            setOf(todo(1).id, todo(2).id, todo(3).id),
            response?.content?.map { it.id }?.toSet()
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
          assertEquals(listOf(todo(3).id), response?.content?.map { it.id })
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

  // get

  @Test
  fun `get returns the todo`() =
    runTest {
      val todo = todo(1)

      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/todos/{todoId}", todo.id)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<TodoResponse>()
        .value { response ->
          assertEquals(todo.id, response?.id)
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
  fun `get another user's todo returns 403`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/todos/{todoId}", todo(4).id)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isForbidden
    }

  // create

  @Test
  fun `create returns the created todo`() =
    runTest {
      val created =
        clientFor(user("user-1"))
          .post()
          .uri("/api/v1/todos")
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(mapOf("title" to "Write integration tests"))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<TodoResponse>()
          .returnResult()
          .responseBody

      assertNotNull(created)
      assertEquals("Write integration tests", created.title)
      assertEquals(false, created.completed)
      assertEquals(user("user-1").identity, todoService.findById(created.id)?.createdBy)
    }

  @Test
  fun `create with invalid title returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .post()
        .uri("/api/v1/todos")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("title" to "abc"))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ApiValidation>()
        .value { response ->
          assertEquals(listOf("title"), response?.violations?.map { it.fieldId })
        }
    }

  @Test
  fun `create with duplicate title is rejected as a client error`() =
    runTest {
      clientFor(user("user-1"))
        .post()
        .uri("/api/v1/todos")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("title" to todo(1).title))
        .exchange()
        .expectStatus()
        .is4xxClientError
    }

  // complete

  @Test
  fun `complete marks the todo as completed`() =
    runTest {
      val todo = todo(1)

      clientFor(user("user-1"))
        .patch()
        .uri("/api/v1/todos/{todoId}/complete", todo.id)
        .exchange()
        .expectStatus()
        .isNoContent
        .expectBody()
        .isEmpty

      assertEquals(true, todoService.findById(todo.id)?.completed)
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
  fun `complete another user's todo returns 403 and leaves it unchanged`() =
    runTest {
      val todo = todo(4)

      clientFor(user("user-1"))
        .patch()
        .uri("/api/v1/todos/{todoId}/complete", todo.id)
        .exchange()
        .expectStatus()
        .isForbidden

      assertFalse(todoService.findById(todo.id)!!.completed)
    }

  @Test
  fun `complete already completed todo returns 409`() =
    runTest {
      val todo = todo(3)

      clientFor(user("user-1"))
        .patch()
        .uri("/api/v1/todos/{todoId}/complete", todo.id)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.CONFLICT)

      assertEquals(true, todoService.findById(todo.id)?.completed)
    }

  @Test
  fun `complete without token returns 401 and leaves the todo unchanged`() =
    runTest {
      val todo = todo(1)

      webTestClient
        .patch()
        .uri("/api/v1/todos/{todoId}/complete", todo.id)
        .exchange()
        .expectStatus()
        .isUnauthorized

      assertFalse(todoService.findById(todo.id)!!.completed)
    }

  // delete

  @Test
  fun `delete removes the todo`() =
    runTest {
      val todo = todo(2)

      clientFor(user("user-1"))
        .delete()
        .uri("/api/v1/todos/{todoId}", todo.id)
        .exchange()
        .expectStatus()
        .isNoContent
        .expectBody()
        .isEmpty

      assertNull(todoService.findById(todo.id))
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
  fun `delete another user's todo returns 403 and keeps it`() =
    runTest {
      val todo = todo(4)

      clientFor(user("user-1"))
        .delete()
        .uri("/api/v1/todos/{todoId}", todo.id)
        .exchange()
        .expectStatus()
        .isForbidden

      assertNotNull(todoService.findById(todo.id))
    }
}
