package io.kostack.skeleton.integration.controller.task

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.spring_tools.dto.ApiValidation
import io.kostack.skeleton.controller.task.response.TaskAssigneeResponse
import io.kostack.skeleton.controller.task.response.TaskListResponse
import io.kostack.skeleton.controller.task.response.TaskResponse
import io.kostack.skeleton.db.r2dbc.enums.TaskStatus
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Fixtures:
 * - project_1 holds task_1 (TODO, user_1), task_2 (IN_PROGRESS, user_2), task_3 (DONE, user_1)
 *   and task_4 (TODO, unassigned)
 * - project_2 holds task_5 (TODO, user_1); project_3 holds no tasks
 * - user_2 is user-2 "Bob Example"; user_3 exists; user-404 does not
 * - project_1 is owned by user_1, project_2 by user_2; tasks can be created in any project
 * - PUT replaces title, status and assigneeId; title and status are required, an omitted or null
 *   assigneeId unassigns the task
 */
class TaskControllerIntegrationTest : BaseIntegrationTest() {
  @Autowired
  private lateinit var taskRepository: TaskRepository

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
  fun `list of an unknown project returns 404`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri { it.path("/api/v1/tasks").queryParam("projectId", "does-not-exist").build() }
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound
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
