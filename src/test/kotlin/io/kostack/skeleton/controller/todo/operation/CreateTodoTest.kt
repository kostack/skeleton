package io.kostack.skeleton.controller.todo.operation

import io.github.kostack.spring_tools.exception.ValidationException
import io.kostack.skeleton.controller.todo.command.TodoCreateCommand
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DuplicateKeyException
import kotlin.test.assertEquals

class CreateTodoTest {
  private val todoRepository = mockk<TodoRepository>()
  private val projectRepository = mockk<ProjectRepository>()
  private val createTodo = CreateTodo(todoRepository, projectRepository)

  private val user = User(identity = "user-1", roles = listOf("ROLE_USER"))
  private val project = Project(id = 7, identifier = "project-1", name = "Project")
  private val title = "Write unit tests"

  @Test
  fun `create saves the todo in the requested project`() =
    runTest {
      coEvery { projectRepository.findByIdentifier(project.identifier) } returns project
      coEvery { todoRepository.save(any()) } answers { firstArg() }

      val created = createTodo(TodoCreateCommand(title = title, projectId = project.identifier), user)

      assertEquals(title, created.title)
      coVerify(exactly = 1) {
        todoRepository.save(match { it.projectId == project.id && it.createdBy == user.identity && it.title == title })
      }
    }

  @Test
  fun `create reports an unknown project as a projectId violation`() =
    runTest {
      coEvery { projectRepository.findByIdentifier("missing") } returns null

      val exception =
        assertThrows<ValidationException> {
          createTodo(TodoCreateCommand(title = title, projectId = "missing"), user)
        }

      assertEquals(listOf("projectId"), exception.violations.map { it.fieldId })
      coVerify(exactly = 0) { todoRepository.save(any()) }
    }

  @Test
  fun `create reports a duplicate title rejected by the unique index as a title violation`() =
    runTest {
      coEvery { projectRepository.findByIdentifier(project.identifier) } returns project
      coEvery { todoRepository.save(any()) } throws DuplicateKeyException("Duplicate entry")

      val exception =
        assertThrows<ValidationException> {
          createTodo(TodoCreateCommand(title = title, projectId = project.identifier), user)
        }

      assertEquals(listOf("title"), exception.violations.map { it.fieldId })
    }
}
