package io.kostack.skeleton.controller.extension

import io.kostack.skeleton.controller.dto.todo.response.TodoListResponse
import io.kostack.skeleton.db.mongodb.model.Todo

fun Todo.toListResponse(): TodoListResponse =
  TodoListResponse(
    id = id,
    title = title
  )
