package io.kostack.skeleton.controller.todo.operation

import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service

@Service
class CompleteTodo(
  private val repo: TodoRepository
) {
  suspend operator fun invoke(todo: Todo) {
    if (todo.completed) {
      return
    }
    try {
      repo.save(todo.copy(completed = true))
    } catch (_: OptimisticLockingFailureException) {
      // A concurrent request completed the todo first
    }
  }
}
