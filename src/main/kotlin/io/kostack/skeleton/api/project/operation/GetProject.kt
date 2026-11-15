package io.kostack.skeleton.api.project.operation

import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.api.project.response.ProjectResponse
import io.kostack.skeleton.api.project.response.toResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@ProjectOperation
class GetProject(
  private val taskRepository: TaskRepository
) {
  @Operation(
    operationId = "project_get_action",
    summary = "Get a project with featured task"
  )
  @GetMapping("/{projectId}", version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("@projectAuth.requireOwner(#project, authentication.principal)")
  suspend operator fun invoke(
    @PathVariable projectId: String,
    @Parameter(hidden = true) @ResolvedArgument project: Project
  ): ProjectResponse {
    val featuredTask = project.featuredTaskId?.let { taskRepository.findById(it) }
    return project.toResponse(featuredTask = featuredTask)
  }
}
