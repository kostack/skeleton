package io.kostack.skeleton.controller.project.operation

import io.kostack.skeleton.controller.project.command.ProjectCreateCommand
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CreateProjectTest {
  private val projectRepository = mockk<ProjectRepository>()
  private val createProject = CreateProject(projectRepository)

  @Test
  fun `create saves a project with the requested name and a new identifier`() =
    runTest {
      coEvery { projectRepository.save(any()) } answers { firstArg() }

      val first = createProject(ProjectCreateCommand(name = "Website"))
      val second = createProject(ProjectCreateCommand(name = "Website"))

      assertEquals("Website", first.name)
      assertNotEquals(first.identifier, second.identifier)
      coVerify(exactly = 2) { projectRepository.save(any()) }
    }
}
