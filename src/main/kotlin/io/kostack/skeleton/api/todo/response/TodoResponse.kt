package io.kostack.skeleton.api.todo.response

import java.time.Instant

data class TodoResponse(
  val identifier: String,
  val title: String,
  val completed: Boolean,
  val lastModifiedAt: Instant
)
