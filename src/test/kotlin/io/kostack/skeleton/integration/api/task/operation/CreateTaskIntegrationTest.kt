package io.kostack.skeleton.integration.api.task.operation

import io.github.kostack.spring_tools.dto.ApiValidation
import io.kostack.skeleton.api.task.response.TaskAssigneeResponse
import io.kostack.skeleton.api.task.response.TaskResponse
import io.kostack.skeleton.db.r2dbc.enums.TaskStatus
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Fixtures:
 * - user_2 is user-2 "Bob Example"; user-404 does not exist
 * - project_1 (owner user_1) and project_2 (owner user_2); tasks can be created in any project
 */
class CreateTaskIntegrationTest : BaseIntegrationTest() {
  @Autowired
  private lateinit var taskRepository: TaskRepository

  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

  @Test
  fun `create returns an unassigned todo task in the project`() =
    runTest {
      val created =
        clientFor(user("user-1"))
          .post()
          .uri("/api/v1/tasks")
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(mapOf("projectId" to project(1).identifier, "title" to "Write tests"))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<TaskResponse>()
          .returnResult()
          .responseBody

      assertNotNull(created)
      assertEquals(project(1).identifier, created.projectId)
      assertEquals("Write tests", created.title)
      assertEquals(TaskStatus.TODO, created.status)
      assertNull(created.assignee)
      val saved = taskRepository.findByIdentifier(created.identifier)
      assertEquals(project(1).id, saved?.projectId)
      assertNull(saved?.assigneeId)
    }

  @Test
  fun `create with an existing assignee returns the assignee summary`() =
    runTest {
      val created =
        clientFor(user("user-1"))
          .post()
          .uri("/api/v1/tasks")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapOf("projectId" to project(2).identifier, "title" to "Review", "assigneeId" to "user-2"))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<TaskResponse>()
          .returnResult()
          .responseBody

      assertEquals(TaskAssigneeResponse(id = "user-2", name = "Bob Example"), created?.assignee)
      assertEquals("user-2", taskRepository.findByIdentifier(created!!.identifier)?.assigneeId)
    }

  @Test
  fun `create with unknown assignee returns 400 and creates nothing`() =
    runTest {
      val before = taskRepository.count()

      clientFor(user("user-1"))
        .post()
        .uri("/api/v1/tasks")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("projectId" to project(1).identifier, "title" to "Ghost work", "assigneeId" to "user-404"))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ApiValidation>()
        .value { response ->
          assertEquals(listOf("assigneeId"), response?.violations?.map { it.fieldId })
        }

      assertEquals(before, taskRepository.count())
    }

  @Test
  fun `create with unknown project returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .post()
        .uri("/api/v1/tasks")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("projectId" to "missing", "title" to "Orphan task"))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ApiValidation>()
        .value { response ->
          assertEquals(listOf("projectId"), response?.violations?.map { it.fieldId })
        }
    }

  @Test
  fun `create with invalid title returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .post()
        .uri("/api/v1/tasks")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("projectId" to project(1).identifier, "title" to " "))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ApiValidation>()
        .value { response ->
          assertEquals(setOf("title"), response?.violations?.map { it.fieldId }?.toSet())
        }
    }

  @Test
  fun `create without token returns 401`() {
    webTestClient
      .post()
      .uri("/api/v1/tasks")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(mapOf("projectId" to "any", "title" to "Write tests"))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
