package io.kostack.skeleton.service.todo

import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo

interface TodoService {
  suspend fun save(todo: Todo): Todo

  suspend fun findByIdentifier(identifier: String): Todo?

  suspend fun findByProject(
    project: Project,
    user: User
  ): List<Todo>

  suspend fun delete(todo: Todo)
}
