package io.kostack.skeleton.api.task.command

import io.kostack.skeleton.db.r2dbc.model.TaskStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.util.Optional

/**
 * Absent fields are left unchanged. `assigneeId: null` unassigns the task; a user identity assigns it.
 */
data class TaskUpdateCommand(
  @field:Size(min = 2, max = 100) val title: String? = null,
  val status: TaskStatus? = null,
  @field:Schema(type = "string", nullable = true, description = "Identity of an existing user, or null to unassign")
  val assigneeId: Optional<String>? = null
)
