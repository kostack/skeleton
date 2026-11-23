package io.kostack.skeleton.controller.task

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.controller.task.command.TaskCreateCommand
import io.kostack.skeleton.controller.task.command.TaskUpdateCommand
import io.kostack.skeleton.controller.task.filter.TaskListFilter
import io.kostack.skeleton.controller.task.response.TaskListResponse
import io.kostack.skeleton.controller.task.response.TaskResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.service.task.TaskService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/{version}/tasks")
@Tag(name = "Task", description = "Api operations for managing project tasks")
class TaskController(
  private val taskService: TaskService
) {
  @Operation(
    operationId = "task_list_action",
    summary = "List a project's tasks, optionally filtered by assignee and status"
  )
  @GetMapping(version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend fun listAction(
    @ParameterObject pageable: Pageable,
    @Valid @ParameterObject filter: TaskListFilter,
    @Parameter(description = "Project identifier") @RequestParam projectId: String,
    @Parameter(hidden = true) @ResolvedArgument project: Project
  ): Pagination<TaskListResponse> = taskService.list(pageable, filter, project)

  @Operation(
    operationId = "task_get_action",
    summary = "Get a task with its assignee"
  )
  @GetMapping("/{taskId}", version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend fun getAction(
    @PathVariable taskId: String,
    @Parameter(hidden = true) @ResolvedArgument task: Task
  ): TaskResponse = taskService.get(task)

  @Operation(
    operationId = "task_create_action",
    summary = "Create a task in a project"
  )
  @PostMapping(
    version = "1+",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
  )
  @ResponseStatus(HttpStatus.CREATED)
  suspend fun createAction(
    @Valid @RequestBody command: TaskCreateCommand
  ): TaskResponse = taskService.create(command.projectId, command.title, command.assigneeId)

  @Operation(
    operationId = "task_update_action",
    summary = "Replace a task's title, status and assignee",
    description = "assigneeId must reference an existing user; null or omitted unassigns the task."
  )
  @PutMapping("/{taskId}", version = "1+", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun updateAction(
    @PathVariable taskId: String,
    @Parameter(hidden = true) @ResolvedArgument task: Task,
    @Valid @RequestBody command: TaskUpdateCommand
  ) {
    try {
      taskService.update(task, command.title, command.status, command.assigneeId)
    } catch (_: OptimisticLockingFailureException) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "The task was modified concurrently")
    }
  }
}
