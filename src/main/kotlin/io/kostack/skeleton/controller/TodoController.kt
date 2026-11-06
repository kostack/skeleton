package io.kostack.skeleton.controller

import io.github.kostack.database_tools.paginator.Pagination
import io.kostack.skeleton.controller.dto.todo.query.TodoListQuery
import io.kostack.skeleton.controller.dto.todo.response.TodoListResponse
import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.service.TodoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Todo", description = "Api operations for managing todo items")
@RequestMapping("/api/todos")
@RestController
class TodoController(
  private val todoService: TodoService
) {
  @Operation(
    operationId = "list_action",
    summary = "List all todos"
  )
  @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend fun listAction(
    @ParameterObject
    @PageableDefault
    pageable: Pageable,
    @ParameterObject
    query: TodoListQuery,
    @AuthenticationPrincipal user: User
  ): Pagination<TodoListResponse> = todoService.list(pageable, query, user)
}
