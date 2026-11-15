package io.kostack.skeleton.integration.api.task.operation

import io.kostack.skeleton.api.task.response.TaskAssigneeResponse
import io.kostack.skeleton.api.task.response.TaskResponse
import io.kostack.skeleton.db.r2dbc.enums.TaskStatus
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
 * - project_1 holds task_2 (IN_PROGRESS, assigned to user_2 "Bob Example") and task_4 (TODO, unassigned)
 */
class GetTaskIntegrationTest : BaseIntegrationTest() {
  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

  private fun task(reference: Int): Task = references.getReference<Task>("task_$reference")

  @Test
  fun `get returns the task with its project and assignee summary`() =
    runTest {
      val task = task(2)

      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/tasks/{taskId}", task.identifier)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<TaskResponse>()
        .value { response ->
          assertEquals(task.identifier, response?.identifier)
          assertEquals(project(1).identifier, response?.projectId)
          assertEquals(task.title, response?.title)
          assertEquals(TaskStatus.IN_PROGRESS, response?.status)
          assertEquals(TaskAssigneeResponse(id = "user-2", name = "Bob Example"), response?.assignee)
        }
    }

  @Test
  fun `get unassigned task returns no assignee`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/tasks/{taskId}", task(4).identifier)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<TaskResponse>()
        .value { response -> assertNull(response?.assignee) }
    }

  @Test
  fun `get does not expose the assignee's email`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/tasks/{taskId}", task(2).identifier)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(String::class.java)
        .value { body -> assertFalse(body!!.contains("@example.com"), body) }
    }

  @Test
  fun `get unknown task returns 404`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri("/api/v1/tasks/{taskId}", "does-not-exist")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound
    }
}
