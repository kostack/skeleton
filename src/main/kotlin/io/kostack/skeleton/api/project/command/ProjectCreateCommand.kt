package io.kostack.skeleton.api.project.command

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ProjectCreateCommand(
  @field:NotBlank @field:Size(min = 2, max = 100) val name: String,
  @field:Size(max = 1000) val description: String? = null
)
