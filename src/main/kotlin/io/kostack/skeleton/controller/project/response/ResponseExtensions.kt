package io.kostack.skeleton.controller.project.response

import io.kostack.skeleton.controller.todo.response.toResponse
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo

fun Project.toResponse(todos: List<Todo> = emptyList()): ProjectResponse =
  ProjectResponse(
    identifier = identifier,
    name = name,
    createdAt = createdAt,
    todos = todos.map { it.toResponse() }
  )
