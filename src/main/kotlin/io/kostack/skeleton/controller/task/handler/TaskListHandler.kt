package io.kostack.skeleton.controller.task.handler

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.database_tools.query_mapper.extension.toPageRequest
import io.github.kostack.database_tools.r2dbc.grid.GridQueryBuilder
import io.github.kostack.database_tools.r2dbc.grid.dsl.table
import io.kostack.skeleton.controller.task.filter.TaskListFilter
import io.kostack.skeleton.controller.task.response.TaskListResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class TaskListHandler(
  private val queryBuilder: GridQueryBuilder
) {
  suspend operator fun invoke(
    pageable: Pageable,
    filter: TaskListFilter,
    project: Project
  ): Pagination<TaskListResponse> {
    val pageRequest = pageable.toPageRequest(TaskListFilter::class)
    val taskTable = table<Task>("t")
    val projectTable = table<Project>("p")
    return queryBuilder
      .from(taskTable)
      .join(projectTable)
      .on(taskTable[Task::projectId] eq projectTable[Project::id])
      .where(taskTable[Task::projectId] eq requireNotNull(project.id))
      .fetchPage<TaskListResponse>(filter, pageRequest)
  }
}
