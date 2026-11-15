package io.kostack.skeleton.service.project

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.controller.project.filter.ProjectListFilter
import io.kostack.skeleton.controller.project.request.ProjectCreateRequest
import io.kostack.skeleton.controller.project.response.ProjectListResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import org.springframework.data.domain.Pageable

interface ProjectService {
  suspend fun save(project: Project): Project

  suspend fun findByIdentifier(identifier: String): Project?

  suspend fun create(projectRequest: ProjectCreateRequest): Project

  suspend fun list(
    pageable: Pageable,
    filter: ProjectListFilter
  ): Pagination<ProjectListResponse>
}
