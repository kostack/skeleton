package io.kostack.skeleton.controller.todo.operation

import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus

@TodoOperation
class CompleteTodo(
  private val todoRepository: TodoRepository
) {
  @Operation(
    operationId = "todo_complete_action",
    summary = "Mark a todo as completed",
    description = "Idempotent: completing an already completed todo also returns 204."
  )
  @PatchMapping("/{todoId}/complete", version = "1+")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend operator fun invoke(
    @PathVariable todoId: String,
    @Parameter(hidden = true) @ResolvedArgument todo: Todo
  ) {
    if (todo.completed) {
      return
    }
    try {
      todoRepository.save(todo.copy(completed = true))
    } catch (_: OptimisticLockingFailureException) {
      // A concurrent request completed the todo first
    }
  }
}
