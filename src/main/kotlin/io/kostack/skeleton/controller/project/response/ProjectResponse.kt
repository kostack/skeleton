package io.kostack.skeleton.controller.project.response

import io.kostack.skeleton.controller.todo.response.TodoResponse
import java.time.Instant

data class ProjectResponse(
  val identifier: String,
  val name: String,
  val createdAt: Instant,
  val todos: List<TodoResponse>
)
