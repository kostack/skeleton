package io.kostack.skeleton.controller.todo.operation

import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import org.springframework.stereotype.Service

@Service
class DeleteTodo(
  private val repo: TodoRepository
) {
  suspend operator fun invoke(todo: Todo) = repo.delete(todo)
}
