package io.kostack.skeleton.api.task.response

import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task

fun Task.toResponse(
  project: Project,
  assignee: User?
): TaskResponse =
  TaskResponse(
    identifier = identifier,
    projectId = project.identifier,
    title = title,
    status = status,
    assignee = assignee?.toAssigneeResponse(),
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt
  )

fun User.toAssigneeResponse(): TaskAssigneeResponse = TaskAssigneeResponse(id = identity, name = name)
