package io.kostack.skeleton.api.project.response

import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task

fun Project.toResponse(featuredTask: Task? = null): ProjectResponse =
  ProjectResponse(
    identifier = identifier,
    name = name,
    description = description,
    featuredTask = featuredTask?.toFeaturedTaskResponse(),
    createdAt = createdAt
  )

fun Task.toFeaturedTaskResponse(): ProjectFeaturedTaskResponse =
  ProjectFeaturedTaskResponse(identifier = identifier, title = title, status = status)
