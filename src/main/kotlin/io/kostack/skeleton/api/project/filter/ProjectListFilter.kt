package io.kostack.skeleton.api.project.filter

import io.github.kostack.database_tools.query_mapper.Aggregate
import io.github.kostack.database_tools.query_mapper.QueryOperator
import io.github.kostack.database_tools.query_mapper.annotation.QueryField
import io.github.kostack.database_tools.query_mapper.annotation.Sortable
import io.github.kostack.database_tools.query_mapper.annotation.SortableDefaults
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Sort

@SortableDefaults(
  defaultSortField = "createdAt",
  defaultSortDirection = Sort.Direction.DESC,
  maxPageSize = 25
)
@Sortable(name = "name")
@Sortable(name = "createdAt")
@Sortable(name = "taskCount")
data class ProjectListFilter(
  @field:Schema(required = false)
  @field:Size(min = 2, max = 100)
  @field:QueryField(operator = QueryOperator.CONTAINS_IGNORE_CASE)
  val name: String? = null,
  @field:Schema(required = false, description = "Only projects with at least this many tasks")
  @field:Min(0)
  @field:QueryField(table = "t", aggregate = Aggregate.COUNT, operator = QueryOperator.GTE)
  val minTaskCount: Long? = null,
  @field:Schema(required = false, description = "Only projects with at most this many tasks")
  @field:Min(0)
  @field:QueryField(table = "t", aggregate = Aggregate.COUNT, operator = QueryOperator.LTE)
  val maxTaskCount: Long? = null
)
