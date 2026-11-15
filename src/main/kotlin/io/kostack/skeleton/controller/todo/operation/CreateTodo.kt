package io.kostack.skeleton.controller.todo.operation

import io.github.kostack.spring_tools.dto.ApiValidation
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.controller.todo.command.TodoCreateCommand
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CreateTodo(
  private val todoRepository: TodoRepository,
  private val projectRepository: ProjectRepository
) {
  suspend operator fun invoke(
    command: TodoCreateCommand,
    user: User
  ): Todo {
    val project =
      projectRepository.findByIdentifier(command.projectId)
        ?: throw ValidationException(
          listOf(ApiValidation.Violation(fieldId = "projectId", message = "Project does not exist"))
        )
    val todo =
      Todo(
        identifier = UUID.randomUUID().toString(),
        title = command.title,
        createdBy = user.identity,
        projectId = requireNotNull(project.id)
      )
    return try {
      todoRepository.save(todo)
    } catch (_: DuplicateKeyException) {
      throw ValidationException(
        listOf(
          ApiValidation.Violation(fieldId = "title", message = "Title already exists")
        )
      )
    }
  }
}
