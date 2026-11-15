package io.kostack.skeleton.service.todo

import io.github.kostack.database_tools.r2dbc.grid.GridQueryBuilder
import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.controller.todo.request.TodoCreateRequest
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import kotlin.test.assertEquals

class TodoServiceImplTest {
  private val todoRepository = mockk<TodoRepository>()
  private val projectRepository = mockk<ProjectRepository>()
  private val todoService = TodoServiceImpl(todoRepository, projectRepository, mockk<GridQueryBuilder>())

  private val user = User(identity = "user-1", roles = listOf("ROLE_USER"))
  private val project = Project(id = 7, identifier = "project-1", name = "Project")
  private val todo =
    Todo(
      id = 1,
      identifier = "todo-1",
      title = "Write unit tests",
      projectId = 7,
      createdBy = user.identity,
      version = 0
    )

  @Test
  fun `findByProject returns the user's todos in the project`() =
    runTest {
      every { todoRepository.findAllByProjectIdAndCreatedByOrderByCreatedAt(7, user.identity) } returns flowOf(todo)

      assertEquals(listOf(todo), todoService.findByProject(project, user))
    }

  @Test
  fun `create saves the todo in the requested project`() =
    runTest {
      coEvery { projectRepository.findByIdentifier(project.identifier) } returns project
      coEvery { todoRepository.save(any()) } answers { firstArg() }

      val created = todoService.create(TodoCreateRequest(title = todo.title, projectId = project.identifier), user)

      assertEquals(project.id, created.projectId)
      assertEquals(user.identity, created.createdBy)
    }

  @Test
  fun `create reports an unknown project as a projectId violation`() =
    runTest {
      coEvery { projectRepository.findByIdentifier("missing") } returns null

      val exception =
        assertThrows<ValidationException> {
          todoService.create(TodoCreateRequest(title = todo.title, projectId = "missing"), user)
        }

      assertEquals(listOf("projectId"), exception.violations.map { it.fieldId })
      coVerify(exactly = 0) { todoRepository.save(any()) }
    }

  @Test
  fun `create reports a duplicate title rejected by the unique index as a title violation`() =
    runTest {
      coEvery { projectRepository.findByIdentifier(project.identifier) } returns project
      coEvery { todoRepository.save(any()) } throws DuplicateKeyException("E11000 duplicate key")

      val exception =
        assertThrows<ValidationException> {
          todoService.create(TodoCreateRequest(title = todo.title, projectId = project.identifier), user)
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
