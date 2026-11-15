package io.kostack.skeleton.api.task.operation

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.database_tools.query_mapper.extension.toPageRequest
import io.github.kostack.database_tools.r2dbc.grid.GridQueryBuilder
import io.github.kostack.database_tools.r2dbc.grid.dsl.table
import io.kostack.skeleton.api.task.filter.TaskListFilter
import io.kostack.skeleton.api.task.response.TaskListResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping

@TaskOperation
class ListTask(
  private val queryBuilder: GridQueryBuilder
) {
  @Operation(
    operationId = "task_list_action",
    summary = "List a project's tasks, optionally filtered by assignee and status"
  )
  @GetMapping(version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend operator fun invoke(
    @ParameterObject pageable: Pageable,
    @Valid @ParameterObject filter: TaskListFilter
  ): Pagination<TaskListResponse> {
    val pageRequest = pageable.toPageRequest(TaskListFilter::class)
    val task = table<Task>("t")
    val project = table<Project>("p")
    return queryBuilder
      .from(task)
      .join(project)
      .on(task[Task::projectId] eq project[Project::id])
      .fetchPage<TaskListResponse>(filter, pageRequest)
  }
}
