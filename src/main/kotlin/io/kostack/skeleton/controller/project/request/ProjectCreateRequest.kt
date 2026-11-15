package io.kostack.skeleton.controller.project.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ProjectCreateRequest(
  @field:NotBlank @field:Size(min = 2, max = 100) val name: String
)
