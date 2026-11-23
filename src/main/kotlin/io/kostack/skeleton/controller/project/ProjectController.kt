package io.kostack.skeleton.controller.project

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.controller.project.command.ProjectCreateCommand
import io.kostack.skeleton.controller.project.command.ProjectUpdateCommand
import io.kostack.skeleton.controller.project.filter.ProjectListFilter
import io.kostack.skeleton.controller.project.response.ProjectListResponse
import io.kostack.skeleton.controller.project.response.ProjectResponse
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.service.project.ProjectService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/{version}/projects")
@Tag(name = "Project", description = "Api operations for managing projects")
class ProjectController(
  private val projectService: ProjectService
) {
  @Operation(
    operationId = "project_list_action",
    summary = "List projects with their task count"
  )
  @GetMapping(version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend fun listAction(
    @ParameterObject pageable: Pageable,
    @Valid @ParameterObject filter: ProjectListFilter,
    @AuthenticationPrincipal principal: User
  ): Pagination<ProjectListResponse> = projectService.list(pageable, filter, principal.identity)

  @Operation(
    operationId = "project_get_action",
    summary = "Get a project with featured task"
  )
  @GetMapping("/{projectId}", version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("@projectAuth.requireOwner(#project, authentication.principal)")
  suspend fun getAction(
    @PathVariable projectId: String,
    @Parameter(hidden = true) @ResolvedArgument project: Project
  ): ProjectResponse = projectService.get(project)

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
  suspend fun createAction(
    @Valid @RequestBody command: ProjectCreateCommand,
    @AuthenticationPrincipal user: User
  ): ProjectResponse = projectService.create(command.name, command.description, user.identity)

  @Operation(
    operationId = "project_update_action",
    summary = "Replace a project's name, description and featured task",
    description =
      "Only the owner may update a project. featuredTaskId must be one of the project's own tasks; " +
        "null or omitted clears it, as does an omitted description."
  )
  @PutMapping("/{projectId}", version = "1+", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@projectAuth.requireOwner(#project, authentication.principal)")
  suspend fun updateAction(
    @PathVariable projectId: String,
    @Parameter(hidden = true) @ResolvedArgument project: Project,
    @Valid @RequestBody command: ProjectUpdateCommand
  ) {
    try {
      projectService.update(project, command.name, command.description, command.featuredTaskId)
    } catch (_: OptimisticLockingFailureException) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "The project was modified concurrently")
    }
  }

  @Operation(
    operationId = "project_delete_action",
    summary = "Delete a project and its tasks",
    description = "Only the owner may delete a project. Its tasks are deleted in the same transaction."
  )
  @DeleteMapping("/{projectId}", version = "1+")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@projectAuth.requireOwner(#project, authentication.principal)")
  suspend fun deleteAction(
    @PathVariable projectId: String,
    @Parameter(hidden = true) @ResolvedArgument project: Project
  ) = projectService.delete(project)
}
