package io.kostack.skeleton.controller.project.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ProjectUpdateRequest(
  @field:NotBlank @field:Size(min = 2, max = 100) val name: String,
  @field:Size(max = 1000) val description: String? = null,
  @field:Schema(type = "string", nullable = true, description = "Task identifier; null or omitted clears it")
  val featuredTaskId: String? = null
)
