package io.kostack.skeleton.controller.task.handler

import io.github.kostack.spring_tools.dto.ApiValidation
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.controller.task.request.TaskCreateRequest
import io.kostack.skeleton.controller.task.response.TaskResponse
import io.kostack.skeleton.controller.task.response.toResponse
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class TaskCreateHandler(
  private val taskRepository: TaskRepository,
  private val projectRepository: ProjectRepository,
  private val userRepository: UserRepository
) {
  suspend operator fun invoke(request: TaskCreateRequest): TaskResponse {
    val project =
      projectRepository.findByIdentifier(request.projectId)
        ?: throw ValidationException(
          listOf(ApiValidation.Violation(fieldId = "projectId", message = "Project does not exist"))
        )
    val assignee =
      request.assigneeId?.let { assigneeId ->
        userRepository.findByIdentity(assigneeId)
          ?: throw ValidationException(
            listOf(ApiValidation.Violation(fieldId = "assigneeId", message = "User does not exist"))
          )
      }
    val task =
      try {
        taskRepository.save(
          Task(
            projectId = requireNotNull(project.id),
            title = request.title,
            assigneeId = assignee?.identity
          )
        )
      } catch (_: DataIntegrityViolationException) {
        // The project was deleted after it was loaded: the foreign key rejects the task
        throw ValidationException(
          listOf(ApiValidation.Violation(fieldId = "projectId", message = "Project does not exist"))
        )
      }
    return task.toResponse(project = project, assignee = assignee)
  }
}
