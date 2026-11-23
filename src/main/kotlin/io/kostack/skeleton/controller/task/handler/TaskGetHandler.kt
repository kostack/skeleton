package io.kostack.skeleton.controller.task.handler

import io.kostack.skeleton.controller.task.response.TaskResponse
import io.kostack.skeleton.controller.task.response.toResponse
import io.kostack.skeleton.db.mongodb.repository.UserRepository
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import org.springframework.stereotype.Service

@Service
class TaskGetHandler(
  private val projectRepository: ProjectRepository,
  private val userRepository: UserRepository
) {
  suspend operator fun invoke(task: Task): TaskResponse {
    val project =
      checkNotNull(projectRepository.findById(task.projectId)) { "Project of task ${task.identifier} is missing" }
    val assignee = task.assigneeId?.let { userRepository.findByIdentity(it) }
    return task.toResponse(project = project, assignee = assignee)
  }
}
