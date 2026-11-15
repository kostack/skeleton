package io.kostack.skeleton.api.todo.operation

import io.github.kostack.spring_tools.dto.ApiValidation
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.api.todo.command.TodoCreateCommand
import io.kostack.skeleton.api.todo.response.TodoResponse
import io.kostack.skeleton.api.todo.response.toResponse
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.UUID

@TodoOperation
class CreateTodo(
  private val todoRepository: TodoRepository,
  private val projectRepository: ProjectRepository
) {
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
  suspend operator fun invoke(
    @Valid @RequestBody command: TodoCreateCommand,
    @AuthenticationPrincipal user: User
  ): TodoResponse {
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
      todoRepository.save(todo).toResponse()
    } catch (_: DuplicateKeyException) {
      throw ValidationException(
        listOf(
          ApiValidation.Violation(fieldId = "title", message = "Title already exists")
        )
      )
    }
  }
}
