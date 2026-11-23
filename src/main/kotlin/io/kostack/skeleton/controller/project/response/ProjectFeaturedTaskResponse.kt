package io.kostack.skeleton.controller.project.response

import io.kostack.skeleton.db.r2dbc.enums.TaskStatus

data class ProjectFeaturedTaskResponse(
  val identifier: String,
  val title: String,
  val status: TaskStatus
)
