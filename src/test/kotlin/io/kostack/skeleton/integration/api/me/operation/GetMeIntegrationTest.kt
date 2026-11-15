package io.kostack.skeleton.integration.api.me.operation

import io.kostack.skeleton.api.me.response.MeResponse
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals

/**
 * Fixtures:
 * - user_1 is user-1 "Alice Example" <user-1@example.com>
 */
class GetMeIntegrationTest : BaseIntegrationTest() {
  @Test
  fun `me returns the current user's id, name and email`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/me")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<MeResponse>()
        .value { response ->
          assertEquals(MeResponse(id = "user-1", name = "Alice Example", email = "user-1@example.com"), response)
        }
    }

  @Test
  fun `me exposes only id, name and email`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/me")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Map<String, Any?>>()
        .value { response ->
          assertEquals(setOf("id", "name", "email"), response?.keys)
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
