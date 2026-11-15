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
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Fixtures:
 * - user_1 is "Alice Example", user_2 is "Bob Example"
 * - project_1 (owner user_1) features task_1; project_2 (owner user_2) features no task
 * - projects are shared: every authenticated user can read all of them
 */
class GetProjectIntegrationTest : BaseIntegrationTest() {
  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

  private fun task(reference: Int): Task = references.getReference<Task>("task_$reference")

  @Test
  fun `get returns the project with its owner and featured task summaries`() =
    runTest {
      val project = project(1)
      val task = task(1)

      clientFor(user("user-2"))
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
          assertEquals(ProjectOwnerResponse(id = "user-1", name = "Alice Example"), response?.owner)
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
        .uri("/api/v1/projects/{projectId}", project(2).identifier)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ProjectResponse>()
        .value { response ->
          assertEquals(ProjectOwnerResponse(id = "user-2", name = "Bob Example"), response?.owner)
          assertNull(response?.featuredTask)
        }
    }

  @Test
  fun `get does not expose the owner's email`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/projects/{projectId}", project(1).identifier)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(String::class.java)
        .value { body -> assertFalse(body!!.contains("@example.com"), body) }
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
