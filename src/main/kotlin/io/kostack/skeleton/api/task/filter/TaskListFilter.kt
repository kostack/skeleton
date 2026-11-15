package io.kostack.skeleton.api.task.filter

import io.github.kostack.database_tools.query_mapper.QueryOperator
import io.github.kostack.database_tools.query_mapper.annotation.QueryField
import io.github.kostack.database_tools.query_mapper.annotation.Sortable
import io.github.kostack.database_tools.query_mapper.annotation.SortableDefaults
import io.kostack.skeleton.db.r2dbc.enums.TaskStatus
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Sort

@SortableDefaults(
  defaultSortField = "createdAt",
  defaultSortDirection = Sort.Direction.DESC,
  maxPageSize = 25
)
@Sortable(name = "title")
@Sortable(name = "status")
@Sortable(name = "createdAt")
data class TaskListFilter(
  @field:Schema(required = false, description = "Identity of the assigned user")
  @field:QueryField(operator = QueryOperator.EQ)
  val assigneeId: String? = null,
  @field:Schema(required = false)
  @field:QueryField(operator = QueryOperator.EQ)
  val status: TaskStatus? = null
)
