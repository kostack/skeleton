package io.kostack.skeleton.api.project.command

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.util.Optional

/**
 * Absent fields are left unchanged. `featuredTaskId: null` clears the featured task; a task identifier
 * selects one of the project's own tasks.
 */
data class ProjectUpdateCommand(
  @field:Size(min = 2, max = 100) val name: String? = null,
  @field:Size(max = 1000) val description: String? = null,
  @field:Schema(type = "string", nullable = true, description = "Task identifier, or null to clear")
  val featuredTaskId: Optional<String>? = null
)
