package io.kostack.skeleton.controller.todo.response

import io.kostack.skeleton.db.mongodb.model.Todo

fun Todo.toListResponse(): TodoListResponse =
  TodoListResponse(
    id = id,
    title = title
  )

fun Todo.toResponse(): TodoResponse =
  TodoResponse(
    id = id,
    title = title,
    completed = completed,
    lastModifiedAt = lastModifiedAt
  )
