package io.kostack.skeleton.integration.controller

import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.integration.BaseControllerIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody

class ApiControllerTest : BaseControllerIntegrationTest() {
  @Test
  fun `authenticated request returns current user`() =
    runTest {
      val token = accessToken("admin-1")
      val (headerName, headerValue) = authorizationHeader(token)

      webTestClient
        .get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/api/me")
            .build()
        }.header(
          headerName,
          headerValue
        ).accept(MediaType.APPLICATION_JSON)
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
  fun `missing token is rejected`() {
    webTestClient
      .get()
      .uri("/api/me")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
