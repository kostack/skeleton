package io.kostack.skeleton.integration.controller

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.spring_tools.dto.ApiValidation
import io.kostack.skeleton.controller.todo.response.TodoListResponse
import io.kostack.skeleton.controller.todo.response.TodoResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.integration.BaseControllerIntegrationTest
import io.kostack.skeleton.service.todo.TodoService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fixtures:
 * - user_1 owns todo_1, todo_2 (open) and todo_3 (completed)
 * - user_2 owns todo_4 (open) and todo_5 (completed)
 * - project_1 holds todo_1, todo_2 and todo_4; project_2 holds todo_3 and todo_5
 */
class TodoControllerTest : BaseControllerIntegrationTest() {
  @Autowired
  private lateinit var todoService: TodoService

  private fun todo(reference: Int): Todo = references.getReference<Todo>("todo_$reference")

  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

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

  // get

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
          .bodyValue(mapOf("title" to "Write integration tests", "projectId" to project(1).identifier))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<TodoResponse>()
          .returnResult()
          .responseBody

      assertNotNull(created)
      assertEquals("Write integration tests", created.title)
      assertEquals(false, created.completed)
      val saved = todoService.findByIdentifier(created.identifier)
      assertEquals(user("user-1").identity, saved?.createdBy)
      assertEquals(project(1).id, saved?.projectId)
    }

  @Test
  fun `create with unknown project returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .post()
        .uri("/api/v1/todos")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("title" to "Orphan todo", "projectId" to "missing"))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ApiValidation>()
        .value { response ->
          assertEquals(listOf("projectId"), response?.violations?.map { it.fieldId })
        }
    }

  @Test
  fun `create with invalid title returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .post()
        .uri("/api/v1/todos")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("title" to "abc", "projectId" to project(1).identifier))
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
        .bodyValue(mapOf("title" to todo(1).title, "projectId" to project(1).identifier))
        .exchange()
        .expectStatus()
        .is4xxClientError
    }

  @Test
  fun `concurrent creates with the same title produce one todo and title violations for the rest`() =
    runTest {
      val client = clientFor(user("user-1"))

      val statuses =
        concurrently(CONCURRENT_REQUESTS) {
          client
            .post()
            .uri("/api/v1/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("title" to "Concurrent todo", "projectId" to project(1).identifier))
            .exchange()
            .returnResult<ApiValidation>()
            .let { it.status to it.responseBody.blockFirst() }
        }

      assertEquals(1, statuses.count { it.first == HttpStatus.CREATED })
      val rejected = statuses.filter { it.first != HttpStatus.CREATED }
      assertEquals(List(CONCURRENT_REQUESTS - 1) { HttpStatus.BAD_REQUEST }, rejected.map { it.first })
      rejected.forEach { (_, body) -> assertEquals(listOf("title"), body?.violations?.map { it.fieldId }) }
    }

  // complete

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

      val stored = todoService.findByIdentifier(todo.identifier)
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

      assertFalse(todoService.findByIdentifier(todo.identifier)!!.completed)
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

      val stored = todoService.findByIdentifier(todo.identifier)
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

      assertFalse(todoService.findByIdentifier(todo.identifier)!!.completed)
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
      val stored = todoService.findByIdentifier(todo.identifier)
      assertEquals(true, stored?.completed)
      assertEquals(todo.version!! + 1, stored?.version)
    }

  // delete

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

      assertNull(todoService.findByIdentifier(todo.identifier))
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

      assertNotNull(todoService.findByIdentifier(todo.identifier))
    }

  companion object {
    private const val CONCURRENT_REQUESTS = 8
  }
}
