package io.kostack.skeleton.integration.controller

import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.integration.BaseControllerIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.expectBody

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ApiControllerTest : BaseControllerIntegrationTest() {
  @Test
  @Order(1)
  fun `should return default message`() =
    runTest {
      dataFixtureManager.loadFixtures(true)

      webTestClient()
        .get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/api/me")
            .queryParam("testId", "ok")
            .build()
        }.header(jwtProperties.authorizationHeaderOptions.name, authorization("admin-1"))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<User>()
        .value { response ->
          assertEquals(
            "admin-1",
            response?.identity
          )
        }
    }
}
