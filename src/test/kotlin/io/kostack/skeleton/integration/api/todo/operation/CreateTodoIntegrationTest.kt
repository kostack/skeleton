package io.kostack.skeleton.integration.api.todo.operation

import io.github.kostack.spring_tools.dto.ApiValidation
import io.kostack.skeleton.api.todo.response.TodoResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Fixtures:
 * - user_1 owns todo_1, todo_2 (open) and todo_3 (completed)
 * - user_2 owns todo_4 (open) and todo_5 (completed)
 * - project_1 holds todo_1, todo_2 and todo_4; project_2 holds todo_3 and todo_5
 */
class CreateTodoIntegrationTest : BaseIntegrationTest() {
  @Autowired
  private lateinit var todoRepository: TodoRepository

  private fun todo(reference: Int): Todo = references.getReference<Todo>("todo_$reference")

  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

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
      val saved = todoRepository.findByIdentifier(created.identifier)
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

  companion object {
    private const val CONCURRENT_REQUESTS = 8
  }
}
