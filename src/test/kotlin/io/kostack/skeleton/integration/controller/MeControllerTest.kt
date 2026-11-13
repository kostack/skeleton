package io.kostack.skeleton.integration.controller

import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.integration.BaseControllerIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody

class MeControllerTest : BaseControllerIntegrationTest() {
  @Test
  fun `authenticated request returns current user v1`() =
    runTest {
      clientFor(user("admin-1"))
        .get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/api/v1/me")
            .build()
        }.accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<String>()
        .value { response ->
          assertEquals(
            "admin-1",
            response
          )
        }
    }

  @Test
  fun `authenticated request returns current user v2`() =
    runTest {
      clientFor(user("admin-1"))
        .get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/api/v2/me")
            .build()
        }.accept(MediaType.APPLICATION_JSON)
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

  @Test
  fun `unsupported api version is rejected`() =
    runTest {
      clientFor(user("admin-1"))
        .get()
        .uri("/api/v3/me")
        .exchange()
        .expectStatus()
        .isBadRequest
    }

  @Test
  fun `missing token is rejected`() {
    webTestClient
      .get()
      .uri("/api/v1/me")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
