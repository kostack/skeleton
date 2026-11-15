package io.kostack.skeleton.integration.controller.project.operation

import io.kostack.skeleton.controller.project.response.ProjectResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo
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
class GetProjectIntegrationTest : BaseIntegrationTest() {
  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

  private fun todo(reference: Int): Todo = references.getReference<Todo>("todo_$reference")

  @Test
  fun `get returns the project with only the current user's todos`() =
    runTest {
      val project = project(1)

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
          assertEquals(
            setOf(todo(1).identifier, todo(2).identifier),
            response?.todos?.map { it.identifier }?.toSet()
          )
        }
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
