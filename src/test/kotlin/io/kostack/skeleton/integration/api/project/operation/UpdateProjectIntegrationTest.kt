package io.kostack.skeleton.integration.api.project.operation

import io.github.kostack.spring_tools.dto.ApiValidation
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
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
 * - project_1 (owner user_1, with a description) holds task_1 to task_4 and features task_1
 * - project_2 (owner user_2) holds task_5 and features no task
 * - PUT replaces name, description and featuredTaskId; an omitted or null value clears it
 */
class UpdateProjectIntegrationTest : BaseIntegrationTest() {
  @Autowired
  private lateinit var projectRepository: ProjectRepository

  @Autowired
  private lateinit var taskRepository: TaskRepository

  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

  private fun task(reference: Int): Task = references.getReference<Task>("task_$reference")

  @Test
  fun `update replaces name and description and keeps a resent featured task`() =
    runTest {
      val project = project(1)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          mapOf("name" to "Renamed", "description" to "New description", "featuredTaskId" to task(1).identifier)
        ).exchange()
        .expectStatus()
        .isNoContent
        .expectBody()
        .isEmpty

      val stored = projectRepository.findByIdentifier(project.identifier)
      assertEquals("Renamed", stored?.name)
      assertEquals("New description", stored?.description)
      assertEquals(task(1).id, stored?.featuredTaskId)
      assertEquals("user-1", stored?.ownerId)
    }

  @Test
  fun `update without description and featuredTaskId clears both and keeps the task`() =
    runTest {
      val project = project(1)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("name" to project.name))
        .exchange()
        .expectStatus()
        .isNoContent

      val stored = projectRepository.findByIdentifier(project.identifier)
      assertNull(stored?.description)
      assertNull(stored?.featuredTaskId)
      assertEquals(task(1).version, taskRepository.findByIdentifier(task(1).identifier)?.version)
    }

  @Test
  fun `update replaces the featured task with another own task and leaves the tasks unchanged`() =
    runTest {
      val project = project(1)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("name" to project.name, "featuredTaskId" to task(2).identifier))
        .exchange()
        .expectStatus()
        .isNoContent

      assertEquals(task(2).id, projectRepository.findByIdentifier(project.identifier)?.featuredTaskId)
      assertEquals(task(1).version, taskRepository.findByIdentifier(task(1).identifier)?.version)
      assertEquals(task(2).version, taskRepository.findByIdentifier(task(2).identifier)?.version)
    }

  @Test
  fun `update with null featuredTaskId clears the selection`() =
    runTest {
      val project = project(1)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""{"name": "${project.name}", "featuredTaskId": null}""")
        .exchange()
        .expectStatus()
        .isNoContent

      assertNull(projectRepository.findByIdentifier(project.identifier)?.featuredTaskId)
    }

  @Test
  fun `update with a task of another project is rejected and keeps the selection`() =
    runTest {
      val project = project(1)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("name" to project.name, "featuredTaskId" to task(5).identifier))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ApiValidation>()
        .value { response ->
          assertEquals(listOf("featuredTaskId"), response?.violations?.map { it.fieldId })
        }

      assertEquals(task(1).id, projectRepository.findByIdentifier(project.identifier)?.featuredTaskId)
    }

  @Test
  fun `update with an unknown featured task is rejected`() =
    runTest {
      val project = project(1)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("name" to project.name, "featuredTaskId" to "does-not-exist"))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ApiValidation>()
        .value { response ->
          assertEquals(listOf("featuredTaskId"), response?.violations?.map { it.fieldId })
        }
    }

  @Test
  fun `update with invalid name returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/projects/{projectId}", project(1).identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("name" to "a"))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ApiValidation>()
        .value { response ->
          assertEquals(listOf("name"), response?.violations?.map { it.fieldId })
        }
    }

  @Test
  fun `update without name returns 400 and leaves the project unchanged`() =
    runTest {
      val project = project(1)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("description" to "Only a description"))
        .exchange()
        .expectStatus()
        .isBadRequest

      assertEquals(project.description, projectRepository.findByIdentifier(project.identifier)?.description)
    }

  @Test
  fun `update by a user who is not the owner returns 403 and leaves the project unchanged`() =
    runTest {
      val project = project(2)

      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/projects/{projectId}", project.identifier)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("name" to "Taken over"))
        .exchange()
        .expectStatus()
        .isForbidden

      assertEquals(project.name, projectRepository.findByIdentifier(project.identifier)?.name)
    }

  @Test
  fun `update unknown project returns 404`() =
    runTest {
      clientFor(user("user-1"))
        .put()
        .uri("/api/v1/projects/{projectId}", "does-not-exist")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("name" to "Renamed"))
        .exchange()
        .expectStatus()
        .isNotFound
    }
}
