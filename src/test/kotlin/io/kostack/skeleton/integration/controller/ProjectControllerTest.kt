package io.kostack.skeleton.integration.controller

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.spring_tools.dto.ApiValidation
import io.kostack.skeleton.controller.project.response.ProjectListResponse
import io.kostack.skeleton.controller.project.response.ProjectResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.integration.BaseControllerIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Fixtures:
 * - project_1 holds todo_1, todo_2 (user_1) and todo_4 (user_2)
 * - project_2 holds todo_3 (user_1) and todo_5 (user_2)
 * - projects are shared: every authenticated user sees all of them
 */
class ProjectControllerTest : BaseControllerIntegrationTest() {
  @Autowired
  private lateinit var projectRepository: ProjectRepository

  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

  private fun todo(reference: Int): Todo = references.getReference<Todo>("todo_$reference")

  // list

  @Test
  fun `list returns all projects with their todo count`() =
    runTest {
      clientFor(user("user-2"))
        .get()
        .uri("/api/v1/projects")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Pagination<ProjectListResponse>>()
        .value { response ->
          assertEquals(2, response?.total)
          assertEquals(
            mapOf(project(1).identifier to 3L, project(2).identifier to 2L),
            response?.content?.associate { it.identifier to it.todoCount }
          )
        }
    }

  @Test
  fun `list filters by name`() =
    runTest {
      clientFor(user("user-1"))
        .get()
        .uri { it.path("/api/v1/projects").queryParam("name", project(2).name).build() }
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Pagination<ProjectListResponse>>()
        .value { response ->
          assertEquals(listOf(project(2).identifier), response?.content?.map { it.identifier })
        }
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

  // get

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

  // create

  @Test
  fun `create returns the created project`() =
    runTest {
      val created =
        clientFor(user("user-1"))
          .post()
          .uri("/api/v1/projects")
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(mapOf("name" to "Website"))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody<ProjectResponse>()
          .returnResult()
          .responseBody

      assertNotNull(created)
      assertEquals("Website", created.name)
      assertEquals(emptyList(), created.todos)
      assertEquals("Website", projectRepository.findByIdentifier(created.identifier)?.name)
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
}
