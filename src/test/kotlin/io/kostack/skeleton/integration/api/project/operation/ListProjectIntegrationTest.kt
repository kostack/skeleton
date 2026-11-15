package io.kostack.skeleton.integration.api.project.operation

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.api.project.response.ProjectListResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals

/**
 * Fixtures:
 * - project_1 and project_3 are owned by user_1, project_2 by user_2
 * - projects are shared: every authenticated user sees all of them
 */
class ListProjectIntegrationTest : BaseIntegrationTest() {
  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

  @Test
  fun `list returns all projects`() =
    runTest {
      clientFor(user("user-2"))
        .get()
        .uri("/api/v1/projects")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Pagination<ProjectListResponse>>()
        .value { response ->
          assertEquals(3, response?.total)
          assertEquals(
            mapOf(
              project(1).identifier to "user-1",
              project(2).identifier to "user-2",
              project(3).identifier to "user-1"
            ),
            response?.content?.associate { it.identifier to it.ownerId }
          )
        }
    }

  @Test
  fun `list filters by owner`() =
    runTest {
      clientFor(user("user-2"))
        .get()
        .uri { it.path("/api/v1/projects").queryParam("ownerId", "user-1").build() }
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Pagination<ProjectListResponse>>()
        .value { response ->
          assertEquals(
            setOf(project(1).identifier, project(3).identifier),
            response?.content?.map { it.identifier }?.toSet()
          )
        }
    }

  @Test
  fun `list filters by name`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri { it.path("/api/v1/projects").queryParam("name", project(2).name).build() }
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Pagination<ProjectListResponse>>()
        .value { response ->
          assertEquals(listOf(project(2).identifier), response?.content?.map { it.identifier })
        }
    }

  @Test
  fun `list with invalid query returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri { it.path("/api/v1/projects").queryParam("name", "a").build() }
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

  @Test
  fun `missing token is rejected`() {
    webTestClient
      .get()
      .uri("/api/v1/projects")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
