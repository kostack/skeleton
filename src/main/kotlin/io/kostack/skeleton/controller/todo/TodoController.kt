package io.kostack.skeleton.controller.todo

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.controller.todo.query.TodoListQuery
import io.kostack.skeleton.controller.todo.request.TodoCreateRequest
import io.kostack.skeleton.controller.todo.response.TodoListResponse
import io.kostack.skeleton.controller.todo.response.TodoResponse
import io.kostack.skeleton.controller.todo.response.toResponse
import io.kostack.skeleton.db.mongodb.model.Todo
import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.service.todo.TodoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Todo", description = "Api operations for managing todo items")
@RequestMapping("/api/{version}/todos")
@RestController
class TodoController(
  private val todoService: TodoService
) {
  @Operation(
    operationId = "todo_list_action",
    summary = "List all todos"
  )
  @GetMapping(version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend fun listAction(
    @ParameterObject pageable: Pageable,
    @Valid @ParameterObject query: TodoListQuery,
    @AuthenticationPrincipal user: User
  ): Pagination<TodoListResponse> = todoService.list(pageable, query, user)

  @Operation(
    operationId = "todo_get_action",
    summary = "Get a todo"
  )
  @GetMapping("/{todoId}", version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("@todoAuth.requireOwner(#todo, authentication.principal)")
  suspend fun getAction(
    @PathVariable todoId: String,
    @Parameter(hidden = true) @ResolvedArgument todo: Todo
  ): TodoResponse = todo.toResponse()

  @Operation(
    operationId = "todo_create_action",
    summary = "Create a todo"
  )
  @PostMapping(
    version = "1+",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
  )
  @ResponseStatus(HttpStatus.CREATED)
  suspend fun createAction(
    @Valid @RequestBody request: TodoCreateRequest,
    @AuthenticationPrincipal user: User
  ): TodoResponse = todoService.create(request, user).toResponse()

  @Operation(
    operationId = "todo_complete_action",
    summary = "Mark a todo as completed",
    responses = [
      ApiResponse(responseCode = "409", description = "Todo is already completed")
    ]
  )
  @PatchMapping("/{todoId}/complete", version = "1+")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@todoAuth.requireOwner(#todo, authentication.principal)")
  suspend fun completeAction(
    @PathVariable todoId: String,
    @Parameter(hidden = true) @ResolvedArgument todo: Todo
  ) = todoService.complete(todo)

  @Operation(
    operationId = "todo_delete_action",
    summary = "Delete a todo"
  )
  @DeleteMapping("/{todoId}", version = "1+")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@todoAuth.requireOwner(#todo, authentication.principal)")
  suspend fun deleteAction(
    @PathVariable todoId: String,
    @Parameter(hidden = true) @ResolvedArgument todo: Todo
  ) = todoService.delete(todo)
}
