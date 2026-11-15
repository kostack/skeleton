package io.kostack.skeleton.api.project.operation

import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.github.kostack.spring_tools.dto.ApiValidation
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.api.project.command.ProjectUpdateCommand
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException

@ProjectOperation
class UpdateProject(
  private val projectRepository: ProjectRepository,
  private val taskRepository: TaskRepository
) {
  @Operation(
    operationId = "project_update_action",
    summary = "Update a project's name, description or featured task",
    description =
      "Only the owner may update a project. featuredTaskId must be one of the project's own tasks; " +
        "null clears it."
  )
  @PatchMapping("/{projectId}", version = "1+", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@projectAuth.requireOwner(#project, authentication.principal)")
  suspend operator fun invoke(
    @PathVariable projectId: String,
    @Parameter(hidden = true) @ResolvedArgument project: Project,
    @Valid @RequestBody command: ProjectUpdateCommand
  ) {
    val featuredTaskId =
      when (val selection = command.featuredTaskId) {
        null -> project.featuredTaskId
        else -> featuredTaskId(project, selection.orElse(null))
      }
    val updated =
      project.copy(
        name = command.name ?: project.name,
        description = command.description ?: project.description,
        featuredTaskId = featuredTaskId
      )
    if (updated == project) {
      return
    }
    try {
      projectRepository.save(updated)
    } catch (_: OptimisticLockingFailureException) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "The project was modified concurrently")
    }
  }

  /** The internal id of [taskIdentifier], which must be one of [project]'s own tasks; null clears the selection. */
  private suspend fun featuredTaskId(
    project: Project,
    taskIdentifier: String?
  ): Long? {
    if (taskIdentifier == null) {
      return null
    }
    val task =
      taskRepository.findByIdentifier(taskIdentifier)?.takeIf { it.projectId == project.id }
        ?: throw ValidationException(
          listOf(ApiValidation.Violation(fieldId = "featuredTaskId", message = "Task does not belong to this project"))
        )
    return task.id
  }
}
