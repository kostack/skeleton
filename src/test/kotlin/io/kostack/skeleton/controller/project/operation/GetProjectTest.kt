package io.kostack.skeleton.controller.project.operation

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

class GetProjectTest {
  private val todoRepository = mockk<TodoRepository>()
  private val getProject = GetProject(todoRepository)

  private val user = User(identity = "user-1", roles = listOf("ROLE_USER"))
  private val project = Project(id = 7, identifier = "project-1", name = "Project")

  @Test
  fun `get returns the project with the current user's todos in it`() =
    runTest {
      val todo =
        Todo(id = 1, identifier = "todo-1", title = "Write unit tests", projectId = 7, createdBy = user.identity)
      every { todoRepository.findAllByProjectIdAndCreatedByOrderByCreatedAt(7, user.identity) } returns flowOf(todo)

      val response = getProject(project.identifier, project, user)

      assertEquals(project.identifier, response.identifier)
      assertEquals(listOf("todo-1"), response.todos.map { it.identifier })
    }
}
