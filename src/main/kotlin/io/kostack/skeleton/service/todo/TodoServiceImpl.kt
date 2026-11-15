package io.kostack.skeleton.service.todo

import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.database_tools.query_mapper.extension.toPageRequest
import io.github.kostack.database_tools.r2dbc.grid.GridQueryBuilder
import io.github.kostack.database_tools.r2dbc.grid.dsl.table
import io.github.kostack.spring_tools.dto.ApiValidation
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.controller.todo.filter.TodoListFilter
import io.kostack.skeleton.controller.todo.request.TodoCreateRequest
import io.kostack.skeleton.controller.todo.response.TodoListResponse
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TodoServiceImpl(
  private val todoRepository: TodoRepository,
  private val queryBuilder: GridQueryBuilder
) : TodoService {
  override suspend fun save(todo: Todo): Todo = todoRepository.save(todo)

  override suspend fun findById(id: Long): Todo? = todoRepository.findById(id)

  override suspend fun findByIdentifier(identifier: String): Todo? = todoRepository.findByIdentifier(identifier)

  override suspend fun findByTitle(title: String): Todo? = todoRepository.findByTitle(title)

  override suspend fun create(
    todoRequest: TodoCreateRequest,
    user: User
  ): Todo {
    val todo = Todo(identifier = UUID.randomUUID().toString(), title = todoRequest.title, createdBy = user.identity)
    return try {
      save(todo)
    } catch (_: DuplicateKeyException) {
      throw ValidationException(
        listOf(
          ApiValidation.Violation(fieldId = "title", message = "Title already exists")
        )
      )
    }
  }

  override suspend fun complete(todo: Todo) {
    if (todo.completed) {
      return
    }
    try {
      save(todo.copy(completed = true))
    } catch (_: OptimisticLockingFailureException) {
      // A concurrent request completed the todo first
    }
  }

  override suspend fun delete(todo: Todo) = todoRepository.delete(todo)

  override suspend fun list(
    pageable: Pageable,
    filter: TodoListFilter,
    user: User
  ): Pagination<TodoListResponse> {
    val pageRequest = pageable.toPageRequest(TodoListFilter::class)
    val todo = table<Todo>("T")
    return queryBuilder
      .from(todo)
      .where(todo[Todo::createdBy] eq user.identity)
      .fetchPage<TodoListResponse>(filter, pageRequest)
  }
}
