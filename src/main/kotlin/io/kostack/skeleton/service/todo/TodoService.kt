package io.kostack.skeleton.service.todo

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.controller.todo.query.TodoListQuery
import io.kostack.skeleton.controller.todo.request.TodoCreateRequest
import io.kostack.skeleton.controller.todo.response.TodoListResponse
import io.kostack.skeleton.db.mongodb.model.Todo
import io.kostack.skeleton.db.r2dbc.model.User
import org.springframework.data.domain.Pageable

interface TodoService {
  suspend fun save(todo: Todo): Todo

  suspend fun findById(id: String): Todo?

  suspend fun findByTitle(title: String): Todo?

  suspend fun create(
    todoRequest: TodoCreateRequest,
    user: User
  ): Todo

  suspend fun complete(todo: Todo): Todo

  suspend fun delete(todo: Todo)

  suspend fun list(
    pageable: Pageable,
    requestQuery: TodoListQuery,
    user: User
  ): Pagination<TodoListResponse>
}
