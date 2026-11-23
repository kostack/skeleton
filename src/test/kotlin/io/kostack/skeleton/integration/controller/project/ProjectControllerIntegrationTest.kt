package io.kostack.skeleton.integration.controller.project

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.spring_tools.dto.ApiValidation
import io.kostack.skeleton.controller.project.response.ProjectFeaturedTaskResponse
import io.kostack.skeleton.controller.project.response.ProjectListResponse
import io.kostack.skeleton.controller.project.response.ProjectResponse
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
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fixtures:
 * - user_1 (user-1) and user_2 (user-2) are stored users
 * - project_1 (owner user_1, with a description) holds task_1 to task_4 and features task_1
 * - project_2 (owner user_2) holds task_5 and features no task
 * - project_3 (owner user_1) holds no tasks and features no task
 * - only the owner may read, update or delete a project; lists are scoped to the current user
 * - PUT replaces name, description and featuredTaskId; an omitted or null optional value clears it
 */
class ProjectControllerIntegrationTest : BaseIntegrationTest() {
  @Autowired
  private lateinit var projectRepository: ProjectRepository

  @Autowired
  private lateinit var taskRepository: TaskRepository

  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

  private fun task(reference: Int): Task = references.getReference<Task>("task_$reference")

  private suspend fun list(
    identity: String,
    vararg params: Pair<String, String>
  ): Pagination<ProjectListResponse>? =
    clientFor(user(identity))
      .get()
      .uri { builder ->
        builder.path("/api/v1/projects")
        params.forEach { (name, value) -> builder.queryParam(name, value) }
        builder.build()
      }.accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<Pagination<ProjectListResponse>>()
      .returnResult()
      .responseBody

  @Test
  fun `create returns the project owned by the current user`() =
    runTest {
      val created =
        clientFor(user("user-1"))
          .post()
          .uri("/api/v1/projects")
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(mapOf("name" to "Website", "description" to "Relaunch"))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<ProjectResponse>()
          .returnResult()
          .responseBody

      assertNotNull(created)
      assertEquals("Website", created.name)
      assertEquals("Relaunch", created.description)
      assertNull(created.featuredTask)
      val saved = projectRepository.findByIdentifier(created.identifier)
      assertEquals("user-1", saved?.ownerId)
      assertNull(saved?.featuredTaskId)
    }

  @Test
  fun `create without description stores none`() =
    runTest {
      val created =
        clientFor(user("user-2"))
          .post()
          .uri("/api/v1/projects")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapOf("name" to "Backlog"))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<ProjectResponse>()
          .returnResult()
          .responseBody

      assertNull(created?.description)
      assertEquals("user-2", projectRepository.findByIdentifier(created!!.identifier)?.ownerId)
    }

  @Test
  fun `create by a user known only from the token owns the project`() =
    runTest {
      val newcomer =
        user(
          "user-9",
          mapOf("name" to "Nina New", "email" to "user-9@example.com", "roles" to listOf("ROLE_USER"))
        )

      val created =
        clientFor(newcomer)
          .post()
          .uri("/api/v1/projects")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapOf("name" to "First project"))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<ProjectResponse>()
          .returnResult()
          .responseBody

      assertEquals("user-9", projectRepository.findByIdentifier(created!!.identifier)?.ownerId)
    }

  @Test
  fun `create ignores a client-supplied ownerId and uses the current user`() =
    runTest {
      val created =
        clientFor(user("user-1"))
          .post()
          .uri("/api/v1/projects")
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(mapOf("name" to "Hijacked", "ownerId" to "user-2"))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<ProjectResponse>()
          .returnResult()
          .responseBody

      assertEquals("user-1", projectRepository.findByIdentifier(created!!.identifier)?.ownerId)
    }

  @Test
  fun `create with invalid name returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .post()
        .uri("/api/v1/projects")
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
  fun `create without token returns 401`() {
    webTestClient
      .post()
      .uri("/api/v1/projects")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(mapOf("name" to "Website"))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

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

  @Test
  fun `list returns only the current user's projects`() =
    runTest {
      val response = list("user-1")

      assertEquals(2, response?.total)
      assertEquals(
        setOf(project(1).identifier, project(3).identifier),
        response?.content?.map { it.identifier }?.toSet()
      )
    }

  @Test
  fun `list does not return other users' projects`() =
    runTest {
      assertEquals(listOf(project(2).identifier), list("user-2")?.content?.map { it.identifier })
    }

  @Test
  fun `list returns the task count of each project`() =
    runTest {
      assertEquals(
        mapOf(project(1).identifier to 4L, project(3).identifier to 0L),
        list("user-1")?.content?.associate { it.identifier to it.taskCount }
      )
    }

  @Test
  fun `list filters by minimum task count`() =
    runTest {
      val response = list("user-1", "minTaskCount" to "1")

      assertEquals(1, response?.total)
      assertEquals(listOf(project(1).identifier), response?.content?.map { it.identifier })
    }

  @Test
  fun `list filters by maximum task count`() =
    runTest {
      val response = list("user-1", "maxTaskCount" to "0")

      assertEquals(1, response?.total)
      assertEquals(listOf(project(3).identifier), response?.content?.map { it.identifier })
    }

  @Test
  fun `list sorts by task count`() =
    runTest {
      assertEquals(
        listOf(project(1).identifier, project(3).identifier),
        list("user-1", "sort" to "taskCount,desc")?.content?.map { it.identifier }
      )
    }

  @Test
  fun `list filters by name`() =
    runTest {
      assertEquals(
        listOf(project(3).identifier),
        list("user-1", "name" to project(3).name)?.content?.map { it.identifier }
      )
    }

  @Test
  fun `list filtered by the name of another user's project is empty`() =
    runTest {
      assertEquals(0, list("user-1", "name" to project(2).name)?.total)
    }

  @Test
  fun `list with negative task count returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri { it.path("/api/v1/projects").queryParam("minTaskCount", "-1").build() }
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

  @Test
  fun `list with invalid query returns 400`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri { it.path("/api/v1/projects").queryParam("name", "a").build() }
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

  @Test
  fun `missing token is rejected`() {
    webTestClient
      .get()
      .uri("/api/v1/projects")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

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

  companion object {
    private const val CONCURRENT_REQUESTS = 8
  }
}
