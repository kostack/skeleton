package io.kostack.skeleton.controller.project.handler

import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectDeleteHandler(
  private val projectRepository: ProjectRepository,
  private val taskRepository: TaskRepository
) {
  @Transactional
  suspend operator fun invoke(project: Project) {
    val id = requireNotNull(project.id)
    projectRepository.findByIdForUpdate(id) ?: return
    taskRepository.deleteAllByProjectId(id)
    projectRepository.deleteById(id)
  }
}
