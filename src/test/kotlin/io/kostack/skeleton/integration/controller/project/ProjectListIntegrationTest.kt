package io.kostack.skeleton.integration.controller.project

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.controller.project.response.ProjectListResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.integration.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals

/**
 * Fixtures:
 * - user_1 owns project_1 (4 tasks) and project_3 (no tasks)
 * - user_2 owns project_2 (1 task); lists are scoped to the current user
 */
class ProjectListIntegrationTest : BaseIntegrationTest() {
  private fun project(reference: Int): Project = references.getReference<Project>("project_$reference")

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
}
