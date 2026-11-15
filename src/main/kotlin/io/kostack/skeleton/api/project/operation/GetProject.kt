package io.kostack.skeleton.api.project.operation

import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.api.project.response.ProjectResponse
import io.kostack.skeleton.api.project.response.toResponse
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.flow.toList
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@ProjectOperation
class GetProject(
  private val todoRepository: TodoRepository
) {
  @Operation(
    operationId = "project_get_action",
    summary = "Get a project with the current user's todos in it"
  )
  @GetMapping("/{projectId}", version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend operator fun invoke(
    @PathVariable projectId: String,
    @Parameter(hidden = true) @ResolvedArgument project: Project,
    @AuthenticationPrincipal user: User
  ): ProjectResponse {
    val todos =
      todoRepository
        .findAllByProjectIdAndCreatedByOrderByCreatedAt(requireNotNull(project.id), user.identity)
        .toList()
    return project.toResponse(todos = todos)
  }
}
