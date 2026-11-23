package io.kostack.skeleton.controller.task.handler

import io.github.kostack.spring_tools.dto.ApiValidation
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.controller.task.request.TaskUpdateRequest
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class TaskUpdateHandler(
  private val taskRepository: TaskRepository,
  private val userRepository: UserRepository
) {
  suspend operator fun invoke(
    task: Task,
    request: TaskUpdateRequest
  ) {
    val assigneeId = request.assigneeId?.let { existingUser(it) }
    val updated =
      task.copy(
        title = request.title,
        status = request.status,
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
