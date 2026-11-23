package io.kostack.skeleton.controller.project.handler

import io.kostack.skeleton.controller.project.response.ProjectResponse
import io.kostack.skeleton.controller.project.response.toResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import org.springframework.stereotype.Service

@Service
class ProjectGetHandler(
  private val taskRepository: TaskRepository
) {
  suspend operator fun invoke(project: Project): ProjectResponse {
    val featuredTask = project.featuredTaskId?.let { taskRepository.findById(it) }
    return project.toResponse(featuredTask = featuredTask)
  }
}
