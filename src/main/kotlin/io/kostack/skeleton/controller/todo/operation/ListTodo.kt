package io.kostack.skeleton.controller.todo.operation

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.database_tools.query_mapper.extension.toPageRequest
import io.github.kostack.database_tools.r2dbc.grid.GridQueryBuilder
import io.github.kostack.database_tools.r2dbc.grid.dsl.table
import io.kostack.skeleton.controller.todo.filter.TodoListFilter
import io.kostack.skeleton.controller.todo.response.TodoListResponse
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Todo
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ListTodo(
  private val queryBuilder: GridQueryBuilder
) {
  suspend operator fun invoke(
    pageable: Pageable,
    filter: TodoListFilter,
    user: User
  ): Pagination<TodoListResponse> {
    val pageRequest = pageable.toPageRequest(TodoListFilter::class)
    val todo = table<Todo>("t")
    return queryBuilder
      .from(todo)
      .where(todo[Todo::createdBy] eq user.identity)
      .fetchPage<TodoListResponse>(filter, pageRequest)
  }
}
