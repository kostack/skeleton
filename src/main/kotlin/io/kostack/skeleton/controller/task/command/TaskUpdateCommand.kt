package io.kostack.skeleton.controller.task.command

import io.kostack.skeleton.db.r2dbc.enums.TaskStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class TaskUpdateCommand(
  @field:Size(min = 2, max = 100) val title: String,
  val status: TaskStatus,
  @field:Schema(
    type = "string",
    nullable = true,
    description = "Identity of an existing user; null or omitted unassigns the task"
  )
  val assigneeId: String? = null
)
