package io.kostack.skeleton.service

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
class TodoService(
  private val todoRepository: TodoRepository,
  private val paginator: MongoDbPaginator
) {
  suspend fun create(
    todoRequest: TodoCreateRequest,
    user: User
  ): Todo {
    val todo = Todo(id = UUID.randomUUID().toString(), title = todoRequest.title, createdBy = user.identity)
    return todoRepository.save(todo)
  }

  suspend fun update(todo: Todo): Todo = todoRepository.save(todo)

  suspend fun list(
    pageable: Pageable,
    requestQuery: TodoListQuery,
    user: User
  ): Pagination<TodoListResponse> {
    val sortedRequest =
      pageable.toPageRequest(
        sortableSource = TodoListQuery::class
      )
    val query =
      Query()
        .toMappedQuery(requestQuery)
        .addCriteria(Criteria.where("createdBy").`is`(user.identity))

    return paginator
      .toPagination(query, sortedRequest, Todo::class)
      .map { todo ->
        todo.toListResponse()
      }
  }
}
