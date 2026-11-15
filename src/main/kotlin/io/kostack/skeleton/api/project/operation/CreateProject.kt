package io.kostack.skeleton.api.project.operation

import io.kostack.skeleton.api.project.command.ProjectCreateCommand
import io.kostack.skeleton.api.project.response.ProjectResponse
import io.kostack.skeleton.api.project.response.toResponse
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus

@ProjectOperation
class CreateProject(
  private val projectRepository: ProjectRepository
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
    val project =
      projectRepository.save(
        Project(
          name = command.name,
          description = command.description,
          ownerId = user.identity
        )
      )
    return project.toResponse()
  }
}
