package io.kostack.skeleton.controller.todo.operation

import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException

class CompleteTodoTest {
  private val todoRepository = mockk<TodoRepository>()
  private val completeTodo = CompleteTodo(todoRepository)

  private val todo =
    Todo(
      id = 1,
      identifier = "todo-1",
      title = "Write unit tests",
      projectId = 7,
      createdBy = "user-1",
      version = 0
    )

  @Test
  fun `complete saves the todo as completed`() =
    runTest {
      coEvery { todoRepository.save(any()) } answers { firstArg() }

      completeTodo(todo.identifier, todo)

      coVerify(exactly = 1) { todoRepository.save(todo.copy(completed = true)) }
    }

  @Test
  fun `complete does nothing when the todo is already completed`() =
    runTest {
      completeTodo(todo.identifier, todo.copy(completed = true))

      coVerify(exactly = 0) { todoRepository.save(any()) }
    }

  @Test
  fun `complete succeeds when a concurrent request completed the todo first`() =
    runTest {
      coEvery { todoRepository.save(any()) } throws OptimisticLockingFailureException("version mismatch")

      completeTodo(todo.identifier, todo)

      coVerify(exactly = 1) { todoRepository.save(any()) }
    }
}
