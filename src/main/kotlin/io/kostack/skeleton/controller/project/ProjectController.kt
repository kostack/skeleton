package io.kostack.skeleton.controller.project

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.controller.project.filter.ProjectListFilter
import io.kostack.skeleton.controller.project.request.ProjectCreateRequest
import io.kostack.skeleton.controller.project.response.ProjectListResponse
import io.kostack.skeleton.controller.project.response.ProjectResponse
import io.kostack.skeleton.controller.project.response.toResponse
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.service.project.ProjectService
import io.kostack.skeleton.service.todo.TodoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Project", description = "Api operations for managing projects")
@RequestMapping("/api/{version}/projects")
@RestController
class ProjectController(
  private val projectService: ProjectService,
  private val todoService: TodoService
) {
  @Operation(
    operationId = "project_list_action",
    summary = "List all projects with their todo count"
  )
  @GetMapping(version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend fun listAction(
    @ParameterObject pageable: Pageable,
    @Valid @ParameterObject query: ProjectListFilter
  ): Pagination<ProjectListResponse> = projectService.list(pageable, query)

  @Operation(
    operationId = "project_get_action",
    summary = "Get a project with the current user's todos in it"
  )
  @GetMapping("/{projectId}", version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend fun getAction(
    @PathVariable projectId: String,
    @Parameter(hidden = true) @ResolvedArgument project: Project,
    @AuthenticationPrincipal user: User
  ): ProjectResponse = project.toResponse(todos = todoService.findByProject(project, user))

  @Operation(
    operationId = "project_create_action",
    summary = "Create a project"
  )
  @PostMapping(
    version = "1+",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
  )
  @ResponseStatus(HttpStatus.CREATED)
  suspend fun createAction(
    @Valid @RequestBody request: ProjectCreateRequest
  ): ProjectResponse = projectService.create(request).toResponse()
}
