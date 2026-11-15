package io.kostack.skeleton.service.todo

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.controller.todo.filter.TodoListFilter
import io.kostack.skeleton.controller.todo.request.TodoCreateRequest
import io.kostack.skeleton.controller.todo.response.TodoListResponse
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Todo
import org.springframework.data.domain.Pageable

interface TodoService {
  suspend fun save(todo: Todo): Todo

  suspend fun findById(id: String): Todo?

  suspend fun findByIdentifier(identifier: String): Todo?

  suspend fun findByTitle(title: String): Todo?

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
