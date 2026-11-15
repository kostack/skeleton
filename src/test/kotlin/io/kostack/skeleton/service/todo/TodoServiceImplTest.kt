package io.kostack.skeleton.service.todo

import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TodoServiceImplTest {
  private val todoRepository = mockk<TodoRepository>()
  private val todoService = TodoServiceImpl(todoRepository)

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
}
