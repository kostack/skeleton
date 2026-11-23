package io.kostack.skeleton.controller.task.command

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TaskCreateCommand(
  @field:NotBlank val projectId: String,
  @field:NotBlank @field:Size(min = 2, max = 100) val title: String,
  @field:Schema(description = "Identity of an existing user")
  val assigneeId: String? = null
)
