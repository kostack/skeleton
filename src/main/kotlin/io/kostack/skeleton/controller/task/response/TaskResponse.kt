package io.kostack.skeleton.controller.task.response

import io.kostack.skeleton.db.r2dbc.enums.TaskStatus
import java.time.Instant

data class TaskResponse(
  val identifier: String,
  val projectId: String,
  val title: String,
  val status: TaskStatus,
  val assignee: TaskAssigneeResponse?,
  val createdAt: Instant,
  val lastModifiedAt: Instant
)
