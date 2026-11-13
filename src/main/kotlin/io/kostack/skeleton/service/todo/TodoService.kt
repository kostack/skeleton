package io.kostack.skeleton.service.todo

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.controller.dto.todo.query.TodoListQuery
import io.kostack.skeleton.controller.dto.todo.request.TodoCreateRequest
import io.kostack.skeleton.controller.dto.todo.response.TodoListResponse
import io.kostack.skeleton.db.mongodb.model.Todo
import io.kostack.skeleton.db.r2dbc.model.User
import org.springframework.data.domain.Pageable

interface TodoService {
  suspend fun create(
    todoRequest: TodoCreateRequest,
    user: User
  ): Todo

  suspend fun update(todo: Todo): Todo

  suspend fun delete(todo: Todo)

  suspend fun list(
    pageable: Pageable,
    requestQuery: TodoListQuery,
    user: User
  ): Pagination<TodoListResponse>
}
