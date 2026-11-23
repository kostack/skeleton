package io.kostack.skeleton.service.project

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.database_tools.query_mapper.extension.toPageRequest
import io.github.kostack.database_tools.r2dbc.grid.GridQueryBuilder
import io.github.kostack.database_tools.r2dbc.grid.dsl.table
import io.github.kostack.spring_tools.dto.ApiValidation
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.controller.project.filter.ProjectListFilter
import io.kostack.skeleton.controller.project.response.ProjectListResponse
import io.kostack.skeleton.controller.project.response.ProjectResponse
import io.kostack.skeleton.controller.project.response.toResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait

@Service
class ProjectServiceImpl(
  private val projectRepository: ProjectRepository,
  private val taskRepository: TaskRepository,
  private val queryBuilder: GridQueryBuilder,
  private val transactionalOperator: TransactionalOperator
) : ProjectService {
  override suspend fun list(
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

  override suspend fun get(project: Project): ProjectResponse {
    val featuredTask = project.featuredTaskId?.let { taskRepository.findById(it) }
    return project.toResponse(featuredTask = featuredTask)
  }

  override suspend fun create(
    name: String,
    description: String?,
    ownerId: String
  ): ProjectResponse {
    val project =
      projectRepository.save(
        Project(
          name = name,
          description = description,
          ownerId = ownerId
        )
      )
    return project.toResponse()
  }

  override suspend fun update(
    project: Project,
    name: String,
    description: String?,
    featuredTaskIdentifier: String?
  ) {
    val updated =
      project.copy(
        name = name,
        description = description,
        featuredTaskId = featuredTaskId(project, featuredTaskIdentifier)
      )
    if (updated == project) {
      return
    }
    projectRepository.save(updated)
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

  override suspend fun delete(project: Project) {
    val id = requireNotNull(project.id)
    transactionalOperator.executeAndAwait {
      projectRepository.findByIdForUpdate(id) ?: return@executeAndAwait
      taskRepository.deleteAllByProjectId(id)
      projectRepository.deleteById(id)
    }
  }
}
