package io.kostack.skeleton.api.project.response

import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task

fun Project.toResponse(
  owner: User,
  featuredTask: Task?
): ProjectResponse =
  ProjectResponse(
    identifier = identifier,
    name = name,
    description = description,
    owner = owner.toOwnerResponse(),
    featuredTask = featuredTask?.toFeaturedTaskResponse(),
    createdAt = createdAt
  )

fun User.toOwnerResponse(): ProjectOwnerResponse = ProjectOwnerResponse(id = identity, name = name)

fun Task.toFeaturedTaskResponse(): ProjectFeaturedTaskResponse =
  ProjectFeaturedTaskResponse(identifier = identifier, title = title, status = status)
