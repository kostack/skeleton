package io.kostack.skeleton.integration.api.project.operation

import io.github.kostack.spring_tools.dto.ApiValidation
import io.kostack.skeleton.api.project.response.ProjectOwnerResponse
import io.kostack.skeleton.api.project.response.ProjectResponse
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
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
 * - user_1 (user-1, "Alice Example") and user_2 (user-2) are stored users
 * - project_1 and project_3 are owned by user_1, project_2 by user_2
 */
class CreateProjectIntegrationTest : BaseIntegrationTest() {
  @Autowired
  private lateinit var projectRepository: ProjectRepository

  @Autowired
  private lateinit var userRepository: UserRepository

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
      assertEquals(ProjectOwnerResponse(id = "user-1", name = "Alice Example"), created.owner)
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
      assertEquals("user-2", created?.owner?.id)
    }

  @Test
  fun `create by a user known only from the token stores that user as owner`() =
    runTest {
      val newcomer =
        user(
          "user-9",
          mapOf("name" to "Nina New", "email" to "user-9@example.com", "roles" to listOf("ROLE_USER"))
        )

      clientFor(newcomer)
        .post()
        .uri("/api/v1/projects")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("name" to "First project"))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody<ProjectResponse>()
        .value { response ->
          assertEquals(ProjectOwnerResponse(id = "user-9", name = "Nina New"), response?.owner)
        }

      assertEquals("Nina New", userRepository.findByIdentity("user-9")?.name)
    }

  @Test
  fun `create with a client-supplied ownerId is rejected`() =
    runTest {
      val before = projectRepository.count()

      clientFor(user("user-1"))
        .post()
        .uri("/api/v1/projects")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("name" to "Hijacked", "ownerId" to "user-2"))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ApiValidation>()
        .value { response ->
          assertEquals(listOf("ownerId"), response?.violations?.map { it.fieldId })
        }

      assertEquals(before, projectRepository.count())
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
}
