package io.kostack.skeleton.integration.controller

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.controller.dto.todo.response.TodoListResponse
import io.kostack.skeleton.integration.BaseControllerIntegrationTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TodoControllerTest : BaseControllerIntegrationTest() {
  @BeforeAll
  fun init() =
    runBlocking {
      dataFixtureManager.loadFixtures(true)
    }

  @Test
  @Order(1)
  fun `list todos`() =
    runTest {
      webTestClient()
        .get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/api/todos")
            .queryParam("completed", "false")
            .queryParam("sort", "title,desc")
            .build()
        }.header(jwtProperties.authorizationHeaderOptions.name, authorization("admin-1"))
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
