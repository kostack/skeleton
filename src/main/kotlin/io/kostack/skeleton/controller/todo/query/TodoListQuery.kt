package io.kostack.skeleton.controller.todo.query

import io.github.kostack.database_tools.query_mapper.QueryOperator
import io.github.kostack.database_tools.query_mapper.annotation.QueryField
import io.github.kostack.database_tools.query_mapper.annotation.Sortable
import io.github.kostack.database_tools.query_mapper.annotation.SortableDefaults
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Sort

@SortableDefaults(
  defaultSortField = "createdAt",
  defaultSortDirection = Sort.Direction.DESC,
  maxPageSize = 10
)
@Sortable(name = "title")
@Sortable(name = "completed")
@Sortable(name = "createdAt")
data class TodoListQuery(
  @field:Schema(required = false)
  @field:Size(min = 2, max = 100)
  @field:QueryField(
    name = "title",
    operator = QueryOperator.CONTAINS_IGNORE_CASE
  )
  val title: String? = null,
  @field:Schema(required = false)
  @field:QueryField(
    name = "completed",
    operator = QueryOperator.EQ
  )
  val completed: Boolean? = null
)
