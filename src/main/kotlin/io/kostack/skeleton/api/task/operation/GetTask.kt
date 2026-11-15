package io.kostack.skeleton.api.task.operation

import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.api.task.response.TaskResponse
import io.kostack.skeleton.api.task.response.toResponse
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@TaskOperation
class GetTask(
  private val projectRepository: ProjectRepository,
  private val userRepository: UserRepository
) {
  @Operation(
    operationId = "task_get_action",
    summary = "Get a task with its assignee"
  )
  @GetMapping("/{taskId}", version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend operator fun invoke(
    @PathVariable taskId: String,
    @Parameter(hidden = true) @ResolvedArgument task: Task
  ): TaskResponse {
    val project = checkNotNull(projectRepository.findById(task.projectId)) { "Project of task $taskId is missing" }
    val assignee = task.assigneeId?.let { userRepository.findByIdentity(it) }
    return task.toResponse(project = project, assignee = assignee)
  }
}
