package io.kostack.skeleton.api.todo.operation

import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.api.todo.response.TodoResponse
import io.kostack.skeleton.api.todo.response.toResponse
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@TodoOperation
class GetTodo {
  @Operation(
    operationId = "todo_get_action",
    summary = "Get a todo"
  )
  @GetMapping("/{todoId}", version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend operator fun invoke(
    @PathVariable todoId: String,
    @Parameter(hidden = true) @ResolvedArgument todo: Todo
  ): TodoResponse = todo.toResponse()
}
