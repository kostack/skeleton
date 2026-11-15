package io.kostack.skeleton.api.task.operation

import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.github.kostack.spring_tools.dto.ApiValidation
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.api.task.command.TaskUpdateCommand
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException

@TaskOperation
class UpdateTask(
  private val taskRepository: TaskRepository,
  private val userRepository: UserRepository
) {
  @Operation(
    operationId = "task_update_action",
    summary = "Update a task's title, status or assignee",
    description = "assigneeId must reference an existing user; null unassigns the task."
  )
  @PatchMapping("/{taskId}", version = "1+", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend operator fun invoke(
    @PathVariable taskId: String,
    @Parameter(hidden = true) @ResolvedArgument task: Task,
    @Valid @RequestBody command: TaskUpdateCommand
  ) {
    val assigneeId =
      when (val selection = command.assigneeId) {
        null -> task.assigneeId
        else -> selection.orElse(null)?.let { existingUser(it) }
      }
    val updated =
      task.copy(
        title = command.title ?: task.title,
        status = command.status ?: task.status,
        assigneeId = assigneeId
      )
    if (updated == task) {
      return
    }
    try {
      taskRepository.save(updated)
    } catch (_: OptimisticLockingFailureException) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "The task was modified concurrently")
    }
  }

  private suspend fun existingUser(identity: String): String =
    userRepository.findByIdentity(identity)?.identity
      ?: throw ValidationException(
        listOf(ApiValidation.Violation(fieldId = "assigneeId", message = "User does not exist"))
      )
}
