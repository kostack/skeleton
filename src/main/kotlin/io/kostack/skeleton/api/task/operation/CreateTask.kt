package io.kostack.skeleton.api.task.operation

import io.github.kostack.spring_tools.dto.ApiValidation
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.api.task.command.TaskCreateCommand
import io.kostack.skeleton.api.task.response.TaskResponse
import io.kostack.skeleton.api.task.response.toResponse
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.UUID

@TaskOperation
class CreateTask(
  private val taskRepository: TaskRepository,
  private val projectRepository: ProjectRepository,
  private val userRepository: UserRepository
) {
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
  suspend operator fun invoke(
    @Valid @RequestBody command: TaskCreateCommand
  ): TaskResponse {
    val project =
      projectRepository.findByIdentifier(command.projectId)
        ?: throw ValidationException(
          listOf(ApiValidation.Violation(fieldId = "projectId", message = "Project does not exist"))
        )
    val assignee =
      command.assigneeId?.let { assigneeId ->
        userRepository.findByIdentity(assigneeId)
          ?: throw ValidationException(
            listOf(ApiValidation.Violation(fieldId = "assigneeId", message = "User does not exist"))
          )
      }
    val task =
      taskRepository.save(
        Task(
          identifier = UUID.randomUUID().toString(),
          projectId = requireNotNull(project.id),
          title = command.title,
          assigneeId = assignee?.identity
        )
      )
    return task.toResponse(project = project, assignee = assignee)
  }
}
