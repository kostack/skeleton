package io.kostack.skeleton.controller.project.response

import java.time.Instant

data class ProjectResponse(
  val identifier: String,
  val name: String,
  val description: String?,
  val featuredTask: ProjectFeaturedTaskResponse?,
  val createdAt: Instant
)
