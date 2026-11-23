package io.kostack.skeleton.integration.controller.project

import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.returnResult
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fixtures:
 * - project_1 (owner user_1) holds task_1 to task_4 and features task_1
 * - project_2 (owner user_2) holds task_5; project_3 (owner user_1) holds no tasks
 * - only the owner may delete a project
 */
class ProjectDeleteIntegrationTest : BaseIntegrationTest() {
  @Autowired
  private lateinit var projectRepository: ProjectRepository

  @Autowired
  private lateinit var taskRepository: TaskRepository

  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

  private fun task(reference: Int): Task = references.getReference<Task>("task_$reference")

  @Test
  fun `delete removes the project and its tasks and keeps other projects' tasks`() =
    runTest {
      val project = project(1)

      clientFor(user("user-1"))
        .delete()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .exchange()
        .expectStatus()
        .isNoContent
        .expectBody()
        .isEmpty

      assertNull(projectRepository.findByIdentifier(project.identifier))
      (1..4).forEach { assertNull(taskRepository.findByIdentifier(task(it).identifier), "task_$it") }
      assertNotNull(taskRepository.findByIdentifier(task(5).identifier))
      assertNotNull(projectRepository.findByIdentifier(project(2).identifier))
    }

  @Test
  fun `delete removes a project without tasks`() =
    runTest {
      val project = project(3)

      clientFor(user("user-1"))
        .delete()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .exchange()
        .expectStatus()
        .isNoContent

      assertNull(projectRepository.findByIdentifier(project.identifier))
    }

  @Test
  fun `delete another user's project returns 403 and keeps the project and its tasks`() =
    runTest {
      val project = project(2)

      clientFor(user("user-1"))
        .delete()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .exchange()
        .expectStatus()
        .isForbidden

      assertNotNull(projectRepository.findByIdentifier(project.identifier))
      assertNotNull(taskRepository.findByIdentifier(task(5).identifier))
    }

  @Test
  fun `delete concurrent with task creation leaves no orphan tasks and no server errors`() =
    runTest {
      val project = project(1)
      val client = clientFor(user("user-1"))
      val request = AtomicInteger()

      val statuses =
        concurrently(CONCURRENT_REQUESTS) {
          if (request.getAndIncrement() == 0) {
            client
              .delete()
              .uri("/api/v1/projects/{projectId}", project.identifier)
              .exchange()
              .returnResult<Void>()
              .status
          } else {
            client
              .post()
              .uri("/api/v1/tasks")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(mapOf("projectId" to project.identifier, "title" to "Racing task"))
              .exchange()
              .returnResult<Void>()
              .status
          }
        }

      assertTrue(HttpStatus.NO_CONTENT in statuses, "delete did not succeed: $statuses")
      assertTrue(
        statuses.all { it in setOf(HttpStatus.NO_CONTENT, HttpStatus.CREATED, HttpStatus.BAD_REQUEST) },
        "unexpected statuses: $statuses"
      )
      assertNull(projectRepository.findByIdentifier(project.identifier))
      assertEquals(0, taskRepository.findAll().toList().count { it.projectId == project.id })
    }

  @Test
  fun `concurrent deletes of the same project succeed or find it gone`() =
    runTest {
      val project = project(1)
      val client = clientFor(user("user-1"))

      val statuses =
        concurrently(CONCURRENT_REQUESTS) {
          client
            .delete()
            .uri("/api/v1/projects/{projectId}", project.identifier)
            .exchange()
            .returnResult<Void>()
            .status
        }

      assertTrue(HttpStatus.NO_CONTENT in statuses, "no delete succeeded: $statuses")
      assertTrue(
        statuses.all { it == HttpStatus.NO_CONTENT || it == HttpStatus.NOT_FOUND },
        "unexpected statuses: $statuses"
      )
      assertNull(projectRepository.findByIdentifier(project.identifier))
    }

  @Test
  fun `delete unknown project returns 404`() =
    runTest {
      clientFor(user("user-1"))
        .delete()
        .uri("/api/v1/projects/{projectId}", "does-not-exist")
        .exchange()
        .expectStatus()
        .isNotFound
    }

  @Test
  fun `delete without token returns 401 and keeps the project`() =
    runTest {
      val project = project(1)

      webTestClient
        .delete()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .exchange()
        .expectStatus()
        .isUnauthorized

      assertNotNull(projectRepository.findByIdentifier(project.identifier))
    }

  companion object {
    private const val CONCURRENT_REQUESTS = 8
  }
}
