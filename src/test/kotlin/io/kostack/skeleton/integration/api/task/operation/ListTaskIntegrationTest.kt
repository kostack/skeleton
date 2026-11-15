package io.kostack.skeleton.integration.api.task.operation

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.api.task.response.TaskListResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals

/**
 * Fixtures:
 * - project_1 holds task_1 (TODO, user_1), task_2 (IN_PROGRESS, user_2), task_3 (DONE, user_1)
 *   and task_4 (TODO, unassigned)
 * - project_2 holds task_5 (TODO, user_1); project_3 holds no tasks
 */
class ListTaskIntegrationTest : BaseIntegrationTest() {
  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

  private fun task(reference: Int): Task = references.getReference<Task>("task_$reference")

  private suspend fun list(vararg params: Pair<String, String>): Pagination<TaskListResponse>? =
    clientFor(user("user-1"))
      .get()
      .uri { builder ->
        builder.path("/api/v1/tasks")
        params.forEach { (name, value) -> builder.queryParam(name, value) }
        builder.build()
      }.accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<Pagination<TaskListResponse>>()
      .returnResult()
      .responseBody

  @Test
  fun `list returns only the tasks of the project`() =
    runTest {
      val response = list("projectId" to project(1).identifier)

      assertEquals(4, response?.total)
      assertEquals(
        setOf(task(1).identifier, task(2).identifier, task(3).identifier, task(4).identifier),
        response?.content?.map { it.identifier }?.toSet()
      )
      assertEquals(setOf(project(1).identifier), response?.content?.map { it.projectId }?.toSet())
    }

  @Test
  fun `list filters by assignee`() =
    runTest {
      val response = list("projectId" to project(1).identifier, "assigneeId" to "user-1")

      assertEquals(setOf(task(1).identifier, task(3).identifier), response?.content?.map { it.identifier }?.toSet())
    }

  @Test
  fun `list filters by status`() =
    runTest {
      val response = list("projectId" to project(1).identifier, "status" to "TODO")

      assertEquals(setOf(task(1).identifier, task(4).identifier), response?.content?.map { it.identifier }?.toSet())
    }

  @Test
  fun `list combines filters with AND`() =
    runTest {
      val response = list("projectId" to project(1).identifier, "assigneeId" to "user-1", "status" to "TODO")

      assertEquals(listOf(task(1).identifier), response?.content?.map { it.identifier })
    }

  @Test
  fun `list of a project without tasks is empty`() =
    runTest {
      assertEquals(0, list("projectId" to project(3).identifier)?.total)
    }

  @Test
  fun `list without projectId returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/tasks")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

  @Test
  fun `list with unknown status returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri {
          it
            .path(
              "/api/v1/tasks"
            ).queryParam("projectId", project(1).identifier)
            .queryParam("status", "LATER")
            .build()
        }.accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

  @Test
  fun `missing token is rejected`() {
    webTestClient
      .get()
      .uri("/api/v1/tasks")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
