package io.kostack.skeleton.service.todo

import io.github.kostack.database_tools.paginator.MongoDbPaginator
import io.github.kostack.database_tools.paginator.Pagination
import io.github.kostack.database_tools.query_mapper.extension.map
import io.github.kostack.database_tools.query_mapper.extension.toMappedQuery
import io.github.kostack.database_tools.query_mapper.extension.toPageRequest
import io.kostack.skeleton.controller.dto.todo.query.TodoListQuery
import io.kostack.skeleton.controller.dto.todo.request.TodoCreateRequest
import io.kostack.skeleton.controller.dto.todo.response.TodoListResponse
import io.kostack.skeleton.controller.extension.toListResponse
import io.kostack.skeleton.db.mongodb.model.Todo
import io.kostack.skeleton.db.mongodb.repository.TodoRepository
import io.kostack.skeleton.db.r2dbc.model.User
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TodoServiceImpl(
  private val todoRepository: TodoRepository,
  private val paginator: MongoDbPaginator
) : TodoService {
  override suspend fun create(
    todoRequest: TodoCreateRequest,
    user: User
  ): Todo {
    val todo = Todo(id = UUID.randomUUID().toString(), title = todoRequest.title, createdBy = user.identity)
    return todoRepository.save(todo)
  }

  override suspend fun update(todo: Todo): Todo = todoRepository.save(todo)

  override suspend fun delete(todo: Todo) = todoRepository.delete(todo)

  override suspend fun list(
    pageable: Pageable,
    requestQuery: TodoListQuery,
    user: User
  ): Pagination<TodoListResponse> {
    val pageRequest = pageable.toPageRequest(TodoListQuery::class)
    val query =
      Query()
        .toMappedQuery(
          requestQuery,
          required = Criteria.where("createdBy").`is`(user.identity)
        )

    return paginator
      .toPagination(query, pageRequest, Todo::class)
      .map { todo ->
        todo.toListResponse()
      }
  }
}
