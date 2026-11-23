package io.kostack.skeleton.controller.project.handler

import io.github.kostack.spring_tools.dto.ApiValidation
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.controller.project.request.ProjectUpdateRequest
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class ProjectUpdateHandler(
  private val projectRepository: ProjectRepository,
  private val taskRepository: TaskRepository
) {
  suspend operator fun invoke(
    project: Project,
    request: ProjectUpdateRequest
  ) {
    val updated =
      project.copy(
        name = request.name,
        description = request.description,
        featuredTaskId = featuredTaskId(project, request.featuredTaskId)
      )
    if (updated == project) {
      return
    }
    try {
      projectRepository.save(updated)
    } catch (_: OptimisticLockingFailureException) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "The project was modified concurrently")
    }
  }

  private suspend fun featuredTaskId(
    project: Project,
    taskIdentifier: String?
  ): Long? {
    if (taskIdentifier == null) {
      return null
    }
    val task =
      taskRepository.findByIdentifier(taskIdentifier)?.takeIf { it.projectId == project.id }
        ?: throw ValidationException(
          listOf(ApiValidation.Violation(fieldId = "featuredTaskId", message = "Task does not belong to this project"))
        )
    return task.id
  }
}
