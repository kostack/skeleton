package io.kostack.skeleton.api.project.response

import io.kostack.skeleton.db.r2dbc.model.TaskStatus

data class ProjectFeaturedTaskResponse(
  val identifier: String,
  val title: String,
  val status: TaskStatus
)
