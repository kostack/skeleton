package io.kostack.skeleton.api.todo.operation

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.database_tools.query_mapper.extension.toPageRequest
import io.github.kostack.database_tools.r2dbc.grid.GridQueryBuilder
import io.github.kostack.database_tools.r2dbc.grid.dsl.table
import io.kostack.skeleton.api.todo.filter.TodoListFilter
import io.kostack.skeleton.api.todo.response.TodoListResponse
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping

@TodoOperation
class ListTodo(
  private val queryBuilder: GridQueryBuilder
) {
  @Operation(
    operationId = "todo_list_action",
    summary = "List all todos"
  )
  @GetMapping(version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend operator fun invoke(
    @ParameterObject pageable: Pageable,
    @Valid @ParameterObject filter: TodoListFilter,
    @AuthenticationPrincipal user: User
  ): Pagination<TodoListResponse> {
    val pageRequest = pageable.toPageRequest(TodoListFilter::class)
    val todo = table<Todo>("t")
    return queryBuilder
      .from(todo)
      .where(todo[Todo::createdBy] eq user.identity)
      .fetchPage<TodoListResponse>(filter, pageRequest)
  }
}
