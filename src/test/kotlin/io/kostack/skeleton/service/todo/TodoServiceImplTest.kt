package io.kostack.skeleton.service.todo

import io.github.kostack.database_tools.paginator.MongoDbPaginator
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.controller.todo.request.TodoCreateRequest
import io.kostack.skeleton.db.mongodb.model.Todo
import io.kostack.skeleton.db.mongodb.repository.TodoRepository
import io.kostack.skeleton.db.r2dbc.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import kotlin.test.assertEquals

class TodoServiceImplTest {
  private val todoRepository = mockk<TodoRepository>()
  private val todoService = TodoServiceImpl(todoRepository, mockk<MongoDbPaginator>())

  private val user = User(identity = "user-1", roles = listOf("ROLE_USER"))
  private val todo = Todo(id = "todo-1", title = "Write unit tests", createdBy = user.identity, version = 0)

  @Test
  fun `create reports a duplicate title rejected by the unique index as a title violation`() =
    runTest {
      coEvery { todoRepository.save(any()) } throws DuplicateKeyException("E11000 duplicate key")

      val exception =
        assertThrows<ValidationException> {
          todoService.create(TodoCreateRequest(title = todo.title), user)
        }

      assertEquals(listOf("title"), exception.violations.map { it.fieldId })
    }

  @Test
  fun `complete saves the todo as completed`() =
    runTest {
      coEvery { todoRepository.save(any()) } answers { firstArg() }

      todoService.complete(todo)

      coVerify(exactly = 1) { todoRepository.save(todo.copy(completed = true)) }
    }

  @Test
  fun `complete does nothing when the todo is already completed`() =
    runTest {
      todoService.complete(todo.copy(completed = true))

      coVerify(exactly = 0) { todoRepository.save(any()) }
    }

  @Test
  fun `complete succeeds when a concurrent request completed the todo first`() =
    runTest {
      coEvery { todoRepository.save(any()) } throws OptimisticLockingFailureException("version mismatch")

      todoService.complete(todo)

      coVerify(exactly = 1) { todoRepository.save(any()) }
    }
}
