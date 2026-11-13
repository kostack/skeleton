package io.kostack.skeleton.integration.controller

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.controller.todo.response.TodoListResponse
import io.kostack.skeleton.controller.todo.response.TodoResponse
import io.kostack.skeleton.integration.BaseControllerIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TodoControllerTest : BaseControllerIntegrationTest() {
  @Test
  @Order(1)
  fun `list todos`() =
    runTest {
      clientFor(user("admin-1"))
        .get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/api/v1/todos")
            .queryParam("completed", "false")
            .queryParam("sort", "title,desc")
            .build()
        }.accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Pagination<TodoListResponse>>()
        .value { response ->
          assertEquals(
            3,
            response?.total
          )
        }
    }

  @Test
  @Order(2)
  fun `get todo`() =
    runTest {
      val client = clientFor(user("admin-1"))
      val listed =
        client
          .get()
          .uri("/api/v1/todos")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<Pagination<TodoListResponse>>()
          .returnResult()
          .responseBody!!
          .content
          .first()

      client
        .get()
        .uri("/api/v1/todos/{todoId}", listed.id)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<TodoResponse>()
        .value { response ->
          assertEquals(listed.id, response?.id)
          assertEquals(listed.title, response?.title)
        }
    }

  @Test
  @Order(3)
  fun `get unknown todo returns 404`() =
    runTest {
      clientFor(user("admin-1"))
        .get()
        .uri("/api/v1/todos/{todoId}", "does-not-exist")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound
    }
}
