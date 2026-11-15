package io.kostack.skeleton.controller.todo.operation

import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class GetTodo(
  private val repo: TodoRepository
) {
  suspend operator fun invoke(identifier: String): Todo =
    repo.findByIdentifier(identifier)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
}
