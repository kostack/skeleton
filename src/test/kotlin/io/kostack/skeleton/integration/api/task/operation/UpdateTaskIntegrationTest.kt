package io.kostack.skeleton.integration.api.task.operation

import io.github.kostack.spring_tools.dto.ApiValidation
import io.kostack.skeleton.db.r2dbc.enums.TaskStatus
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Fixtures:
 * - task_1 (project_1, TODO, assigned to user_1); task_4 (project_1, TODO, unassigned)
 * - user_2 and user_3 exist; user-404 does not
 * - PUT replaces title, status and assigneeId; title and status are required, an omitted or null
 *   assigneeId unassigns the task
 */
class UpdateTaskIntegrationTest : BaseIntegrationTest() {
  @Autowired
  private lateinit var taskRepository: TaskRepository

  private fun task(reference: Int): Task = references.getReference<Task>("task_$reference")

  @Test
  fun `update replaces title and status and keeps a resent assignee`() =
    runTest {
      val task = task(1)

      clientFor(user("user-2"))
        .put()
        .uri("/api/v1/tasks/{taskId}", task.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("title" to "Renamed task", "status" to "IN_PROGRESS", "assigneeId" to "user-1"))
        .exchange()
        .expectStatus()
        .isNoContent
        .expectBody()
        .isEmpty

      val stored = taskRepository.findByIdentifier(task.identifier)
      assertEquals("Renamed task", stored?.title)
      assertEquals(TaskStatus.IN_PROGRESS, stored?.status)
      assertEquals("user-1", stored?.assigneeId)
      assertEquals(task.projectId, stored?.projectId)
    }

  @Test
  fun `update assigns an existing user`() =
    runTest {
      val task = task(4)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/tasks/{taskId}", task.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("title" to task.title, "status" to task.status.name, "assigneeId" to "user-3"))
        .exchange()
        .expectStatus()
        .isNoContent

      assertEquals("user-3", taskRepository.findByIdentifier(task.identifier)?.assigneeId)
    }

  @Test
  fun `update with null assigneeId unassigns the task`() =
    runTest {
      val task = task(1)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/tasks/{taskId}", task.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""{"title": "${task.title}", "status": "DONE", "assigneeId": null}""")
        .exchange()
        .expectStatus()
        .isNoContent

      val stored = taskRepository.findByIdentifier(task.identifier)
      assertNull(stored?.assigneeId)
      assertEquals(TaskStatus.DONE, stored?.status)
    }

  @Test
  fun `update without assigneeId unassigns the task`() =
    runTest {
      val task = task(1)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/tasks/{taskId}", task.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("title" to task.title, "status" to task.status.name))
        .exchange()
        .expectStatus()
        .isNoContent

      assertNull(taskRepository.findByIdentifier(task.identifier)?.assigneeId)
    }

  @Test
  fun `update with unknown assignee returns 400 and keeps the assignee`() =
    runTest {
      val task = task(1)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/tasks/{taskId}", task.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("title" to task.title, "status" to task.status.name, "assigneeId" to "user-404"))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ApiValidation>()
        .value { response ->
          assertEquals(listOf("assigneeId"), response?.violations?.map { it.fieldId })
        }

      assertEquals("user-1", taskRepository.findByIdentifier(task.identifier)?.assigneeId)
    }

  @Test
  fun `update without status returns 400 and leaves the task unchanged`() =
    runTest {
      val task = task(1)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/tasks/{taskId}", task.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("title" to "Renamed task"))
        .exchange()
        .expectStatus()
        .isBadRequest

      assertEquals(task.title, taskRepository.findByIdentifier(task.identifier)?.title)
    }

  @Test
  fun `update with unknown status returns 400`() =
    runTest {
      val task = task(1)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/tasks/{taskId}", task.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("title" to task.title, "status" to "LATER"))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

  @Test
  fun `update unknown task returns 404`() =
    runTest {
      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/tasks/{taskId}", "does-not-exist")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("title" to "Renamed task", "status" to "TODO"))
        .exchange()
        .expectStatus()
        .isNotFound
    }
}
