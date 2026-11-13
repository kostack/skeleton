package io.kostack.skeleton.integration.controller

import io.kostack.skeleton.integration.BaseControllerIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class OpenApiDocsTest : BaseControllerIntegrationTest() {
  @Test
  fun `document lists every api version`() =
    runTest {
      clientFor(user("admin-1"))
        .get()
        .uri("/v3/api-docs")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.paths['/api/v1/me'].get")
        .exists()
        .jsonPath("$.paths['/api/v2/me'].get")
        .exists()
        .jsonPath("$.paths['/api/v1/todos'].get")
        .exists()
    }
}
