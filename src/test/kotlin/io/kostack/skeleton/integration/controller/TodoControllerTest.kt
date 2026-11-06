package io.kostack.skeleton.integration.controller

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.controller.dto.todo.response.TodoListResponse
import io.kostack.skeleton.integration.BaseControllerIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals

class TodoControllerTest : BaseControllerIntegrationTest() {
  @Test
  @Order(2)
  fun `list todos`() =
    runTest {
      val token = accessToken("admin-1")
      val (headerName, headerValue) = authorizationHeader(token)

      webTestClient
        .get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/api/todos")
            .queryParam("completed", "false")
            .queryParam("sort", "title,desc")
            .build()
        }.header(headerName, headerValue)
        .accept(MediaType.APPLICATION_JSON)
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
}
