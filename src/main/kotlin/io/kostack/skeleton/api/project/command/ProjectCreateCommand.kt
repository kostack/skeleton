package io.kostack.skeleton.api.project.command

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Null
import jakarta.validation.constraints.Size

data class ProjectCreateCommand(
  @field:NotBlank @field:Size(min = 2, max = 100) val name: String,
  @field:Size(max = 1000) val description: String? = null,
  /** Bound only to reject it: the owner is always the authenticated user. */
  @field:Schema(hidden = true)
  @field:Null(message = "ownerId is set from the authenticated user and must not be supplied")
  val ownerId: Any? = null
)
