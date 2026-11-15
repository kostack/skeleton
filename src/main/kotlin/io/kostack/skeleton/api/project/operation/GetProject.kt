package io.kostack.skeleton.api.project.operation

import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.api.project.response.ProjectResponse
import io.kostack.skeleton.api.project.response.toResponse
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@ProjectOperation
class GetProject(
  private val userRepository: UserRepository,
  private val taskRepository: TaskRepository
) {
  @Operation(
    operationId = "project_get_action",
    summary = "Get a project with its owner and featured task"
  )
  @GetMapping("/{projectId}", version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend operator fun invoke(
    @PathVariable projectId: String,
    @Parameter(hidden = true) @ResolvedArgument project: Project
  ): ProjectResponse {
    val owner =
      checkNotNull(userRepository.findByIdentity(project.ownerId)) { "Owner of project $projectId is missing" }
    val featuredTask = project.featuredTaskId?.let { taskRepository.findById(it) }
    return project.toResponse(owner = owner, featuredTask = featuredTask)
  }
}
