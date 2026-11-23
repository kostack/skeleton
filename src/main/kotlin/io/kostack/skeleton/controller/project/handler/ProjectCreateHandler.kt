package io.kostack.skeleton.controller.project.handler

import io.kostack.skeleton.controller.project.request.ProjectCreateRequest
import io.kostack.skeleton.controller.project.response.ProjectResponse
import io.kostack.skeleton.controller.project.response.toResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import org.springframework.stereotype.Service

@Service
class ProjectCreateHandler(
  private val projectRepository: ProjectRepository
) {
  suspend operator fun invoke(
    request: ProjectCreateRequest,
    ownerId: String
  ): ProjectResponse {
    val project =
      projectRepository.save(
        Project(
          name = request.name,
          description = request.description,
          ownerId = ownerId
        )
      )
    return project.toResponse()
  }
}
