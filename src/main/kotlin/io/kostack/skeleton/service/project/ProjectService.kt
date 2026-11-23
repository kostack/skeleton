package io.kostack.skeleton.service.project

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.controller.project.filter.ProjectListFilter
import io.kostack.skeleton.controller.project.response.ProjectListResponse
import io.kostack.skeleton.controller.project.response.ProjectResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import org.springframework.data.domain.Pageable

interface ProjectService {
  suspend fun list(
    pageable: Pageable,
    filter: ProjectListFilter,
    ownerId: String
  ): Pagination<ProjectListResponse>

  suspend fun get(project: Project): ProjectResponse

  suspend fun create(
    name: String,
    description: String?,
    ownerId: String
  ): ProjectResponse

  suspend fun update(
    project: Project,
    name: String,
    description: String?,
    featuredTaskIdentifier: String?
  )

  suspend fun delete(project: Project)
}
