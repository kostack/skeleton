package io.kostack.skeleton.controller.todo.operation

import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus

@TodoOperation
class DeleteTodo(
  private val todoRepository: TodoRepository
) {
  @Operation(
    operationId = "todo_delete_action",
    summary = "Delete a todo"
  )
  @DeleteMapping("/{todoId}", version = "1+")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend operator fun invoke(
    @PathVariable todoId: String,
    @Parameter(hidden = true) @ResolvedArgument todo: Todo
  ) = todoRepository.delete(todo)
}
