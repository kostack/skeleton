package io.kostack.skeleton.integration.api.project.operation

import io.github.kostack.spring_tools.dto.ApiValidation
import io.kostack.skeleton.api.project.response.ProjectResponse
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.integration.BaseIntegrationTest
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
class CreateProjectIntegrationTest : BaseIntegrationTest() {
  @Autowired
  private lateinit var projectRepository: ProjectRepository

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
