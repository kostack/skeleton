package io.kostack.skeleton.api.todo.command

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TodoCreateCommand(
  @field:NotBlank @field:Size(min = 5, max = 100) val title: String,
  @field:NotBlank val projectId: String
)
