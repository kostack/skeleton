package io.kostack.skeleton.integration.api.me.operation

import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody

class GetMeIntegrationTest : BaseIntegrationTest() {
  @Test
  fun `me returns the current user`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/api/v1/me")
            .build()
        }.accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<User>()
        .value { response ->
          assertEquals(
            "user-1",
            response?.identity
          )
        }
    }

  @Test
  fun `unsupported api version is rejected`() =
    runTest {
      clientFor(user("user-1"))
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
