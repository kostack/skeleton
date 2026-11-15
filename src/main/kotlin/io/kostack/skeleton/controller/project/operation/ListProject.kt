package io.kostack.skeleton.controller.project.operation

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.database_tools.query_mapper.extension.toPageRequest
import io.github.kostack.database_tools.r2dbc.grid.GridQueryBuilder
import io.github.kostack.database_tools.r2dbc.grid.dsl.table
import io.kostack.skeleton.controller.project.filter.ProjectListFilter
import io.kostack.skeleton.controller.project.response.ProjectListResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping

@ProjectOperation
class ListProject(
  private val queryBuilder: GridQueryBuilder
) {
  @Operation(
    operationId = "project_list_action",
    summary = "List all projects with their todo count"
  )
  @GetMapping(version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend operator fun invoke(
    @ParameterObject pageable: Pageable,
    @Valid @ParameterObject filter: ProjectListFilter
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
