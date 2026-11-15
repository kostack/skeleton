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
 * - project_1 holds todo_1, todo_2 (user_1) and todo_4 (user_2)
 * - project_2 holds todo_3 (user_1) and todo_5 (user_2)
 * - projects are shared: every authenticated user sees all of them
 */
class ListProjectIntegrationTest : BaseIntegrationTest() {
  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

  @Test
  fun `list returns all projects with their todo count`() =
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
          assertEquals(2, response?.total)
          assertEquals(
            mapOf(project(1).identifier to 3L, project(2).identifier to 2L),
            response?.content?.associate { it.identifier to it.todoCount }
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
