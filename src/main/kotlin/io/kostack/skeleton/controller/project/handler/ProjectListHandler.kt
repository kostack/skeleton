package io.kostack.skeleton.controller.project.handler

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.database_tools.query_mapper.extension.toPageRequest
import io.github.kostack.database_tools.r2dbc.grid.GridQueryBuilder
import io.github.kostack.database_tools.r2dbc.grid.dsl.table
import io.kostack.skeleton.controller.project.filter.ProjectListFilter
import io.kostack.skeleton.controller.project.response.ProjectListResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ProjectListHandler(
  private val queryBuilder: GridQueryBuilder
) {
  suspend operator fun invoke(
    pageable: Pageable,
    filter: ProjectListFilter,
    ownerId: String
  ): Pagination<ProjectListResponse> {
    val pageRequest = pageable.toPageRequest(ProjectListFilter::class)
    val project = table<Project>("p")
    val task = table<Task>("t")
    return queryBuilder
      .from(project)
      .where(project[Project::ownerId] eq ownerId)
      .aggregateJoin(task)
      .on(project[Project::id] eq task[Task::projectId])
      .fetchPage<ProjectListResponse>(filter, pageRequest)
  }
}
