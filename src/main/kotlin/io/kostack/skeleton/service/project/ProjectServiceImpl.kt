package io.kostack.skeleton.service.project

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.database_tools.query_mapper.extension.toPageRequest
import io.github.kostack.database_tools.r2dbc.grid.GridQueryBuilder
import io.github.kostack.database_tools.r2dbc.grid.dsl.table
import io.kostack.skeleton.controller.project.filter.ProjectListFilter
import io.kostack.skeleton.controller.project.request.ProjectCreateRequest
import io.kostack.skeleton.controller.project.response.ProjectListResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProjectServiceImpl(
  private val projectRepository: ProjectRepository,
  private val queryBuilder: GridQueryBuilder
) : ProjectService {
  override suspend fun save(project: Project): Project = projectRepository.save(project)

  override suspend fun findByIdentifier(identifier: String): Project? = projectRepository.findByIdentifier(identifier)

  override suspend fun create(projectRequest: ProjectCreateRequest): Project =
    save(Project(identifier = UUID.randomUUID().toString(), name = projectRequest.name))

  override suspend fun list(
    pageable: Pageable,
    filter: ProjectListFilter
  ): Pagination<ProjectListResponse> {
    val pageRequest = pageable.toPageRequest(ProjectListFilter::class)
    val project = table<Project>("p")
    val todo = table<Todo>("t")
    return queryBuilder
      .from(project)
      .aggregateJoin(todo)
      .on(project[Project::id] eq todo[Todo::projectId])
      .fetchPage<ProjectListResponse>(filter, pageRequest)
  }
}
