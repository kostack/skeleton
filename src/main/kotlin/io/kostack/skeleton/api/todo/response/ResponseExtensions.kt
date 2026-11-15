package io.kostack.skeleton.api.todo.response

import io.kostack.skeleton.db.r2dbc.model.Todo

fun Todo.toResponse(): TodoResponse =
  TodoResponse(
    identifier = identifier,
    title = title,
    completed = completed,
    lastModifiedAt = lastModifiedAt
  )
