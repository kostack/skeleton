package io.kostack.skeleton.service.todo

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.controller.todo.filter.TodoListFilter
import io.kostack.skeleton.controller.todo.request.TodoCreateRequest
import io.kostack.skeleton.controller.todo.response.TodoListResponse
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo
import org.springframework.data.domain.Pageable

interface TodoService {
  suspend fun save(todo: Todo): Todo

  suspend fun findByIdentifier(identifier: String): Todo?

  /** The [user]'s todos in [project], oldest first. Other users' todos are never included. */
  suspend fun findByProject(
    project: Project,
    user: User
  ): List<Todo>

  suspend fun create(
    todoRequest: TodoCreateRequest,
    user: User
  ): Todo

  suspend fun complete(todo: Todo)

  suspend fun delete(todo: Todo)

  suspend fun list(
    pageable: Pageable,
    filter: TodoListFilter,
    user: User
  ): Pagination<TodoListResponse>
}
