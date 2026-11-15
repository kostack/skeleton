package io.kostack.skeleton.api.project.filter

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
  maxPageSize = 25
)
@Sortable(name = "name")
@Sortable(name = "createdAt")
data class ProjectListFilter(
  @field:Schema(required = false)
  @field:Size(min = 2, max = 100)
  @field:QueryField(operator = QueryOperator.CONTAINS_IGNORE_CASE)
  val name: String? = null,
  @field:Schema(required = false, description = "Identity of the owning user")
  @field:QueryField(operator = QueryOperator.EQ)
  val ownerId: String? = null
)
