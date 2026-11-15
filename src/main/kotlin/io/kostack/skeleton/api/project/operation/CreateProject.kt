package io.kostack.skeleton.api.project.operation

import io.kostack.skeleton.api.project.command.ProjectCreateCommand
import io.kostack.skeleton.api.project.response.ProjectResponse
import io.kostack.skeleton.api.project.response.toResponse
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@ProjectOperation
class CreateProject(
  private val projectRepository: ProjectRepository,
  private val userRepository: UserRepository
) {
  @Operation(
    operationId = "project_create_action",
    summary = "Create a project owned by the current user"
  )
  @PostMapping(
    version = "1+",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
  )
  @ResponseStatus(HttpStatus.CREATED)
  suspend operator fun invoke(
    @Valid @RequestBody command: ProjectCreateCommand,
    @AuthenticationPrincipal user: User
  ): ProjectResponse {
    val owner = stored(user)
    val project =
      projectRepository.save(
        Project(
          identifier = UUID.randomUUID().toString(),
          name = command.name,
          description = command.description,
          ownerId = owner.identity
        )
      )
    return project.toResponse(owner = owner, featuredTask = null)
  }

  /**
   * A token-only user (not stored yet) is stored on their first project, so that every owner can be
   * resolved to a summary later.
   */
  private suspend fun stored(user: User): User {
    if (user.id != null) {
      return user
    }
    return try {
      userRepository.save(user)
    } catch (_: DuplicateKeyException) {
      // A concurrent request stored the same user first; an email owned by another identity is a conflict
      userRepository.findByIdentity(user.identity) ?: throw ResponseStatusException(HttpStatus.CONFLICT)
    }
  }
}
