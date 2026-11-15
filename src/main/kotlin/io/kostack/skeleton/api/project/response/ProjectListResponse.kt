package io.kostack.skeleton.api.project.response

import io.github.kostack.database_tools.query_mapper.Aggregate
import io.github.kostack.database_tools.query_mapper.annotation.SelectAggregate

data class ProjectListResponse(
  val identifier: String,
  val name: String,
  @SelectAggregate(table = "t", function = Aggregate.COUNT) val todoCount: Long
)
