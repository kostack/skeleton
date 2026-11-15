package io.kostack.skeleton.integration.api.project.operation

import io.kostack.skeleton.api.project.response.ProjectFeaturedTaskResponse
import io.kostack.skeleton.api.project.response.ProjectResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Fixtures:
 * - project_1 (owner user_1) features task_1; project_3 (owner user_1) features no task
 * - project_2 is owned by user_2
 * - only the owner may read a project
 */
class GetProjectIntegrationTest : BaseIntegrationTest() {
  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

  private fun task(reference: Int): Task = references.getReference<Task>("task_$reference")

  @Test
  fun `get returns the project with its featured task summary`() =
    runTest {
      val project = project(1)
      val task = task(1)

      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ProjectResponse>()
        .value { response ->
          assertEquals(project.identifier, response?.identifier)
          assertEquals(project.name, response?.name)
          assertEquals(
            ProjectFeaturedTaskResponse(identifier = task.identifier, title = task.title, status = task.status),
            response?.featuredTask
          )
        }
    }

  @Test
  fun `get returns no featured task when none is selected`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/projects/{projectId}", project(3).identifier)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ProjectResponse>()
        .value { response -> assertNull(response?.featuredTask) }
    }

  @Test
  fun `get another user's project returns 403`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/projects/{projectId}", project(2).identifier)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isForbidden
    }

  @Test
  fun `get unknown project returns 404`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/projects/{projectId}", "does-not-exist")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound
    }
}
