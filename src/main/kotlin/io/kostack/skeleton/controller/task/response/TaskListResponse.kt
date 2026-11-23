package io.kostack.skeleton.controller.task.response

import io.github.kostack.database_tools.query_mapper.annotation.SelectColumn
import io.kostack.skeleton.db.r2dbc.enums.TaskStatus

data class TaskListResponse(
  val identifier: String,
  @SelectColumn(table = "p", name = "identifier") val projectId: String,
  val title: String,
  val status: TaskStatus,
  val assigneeId: String?
)
