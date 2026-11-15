package io.kostack.skeleton.service.todo

import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import kotlinx.coroutines.flow.toList
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service

@Service
class TodoServiceImpl(
  private val todoRepository: TodoRepository
) : TodoService {
  override suspend fun save(todo: Todo): Todo = todoRepository.save(todo)

  override suspend fun findByIdentifier(identifier: String): Todo? = todoRepository.findByIdentifier(identifier)

  override suspend fun findByProject(
    project: Project,
    user: User
  ): List<Todo> =
    todoRepository
      .findAllByProjectIdAndCreatedByOrderByCreatedAt(requireNotNull(project.id), user.identity)
      .toList()

  override suspend fun delete(todo: Todo) = todoRepository.delete(todo)
}
