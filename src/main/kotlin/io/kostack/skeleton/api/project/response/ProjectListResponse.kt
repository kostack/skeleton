package io.kostack.skeleton.api.project.response

data class ProjectListResponse(
  val identifier: String,
  val name: String,
  val description: String?,
  val ownerId: String
)
