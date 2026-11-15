package io.kostack.skeleton.service.project

import io.github.kostack.database_tools.r2dbc.grid.GridQueryBuilder
import io.kostack.skeleton.controller.project.request.ProjectCreateRequest
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ProjectServiceImplTest {
  private val projectRepository = mockk<ProjectRepository>()
  private val projectService = ProjectServiceImpl(projectRepository, mockk<GridQueryBuilder>())

  @Test
  fun `findByIdentifier returns the stored project`() =
    runTest {
      val project = Project(id = 1, identifier = "project-1", name = "Website")
      coEvery { projectRepository.findByIdentifier(project.identifier) } returns project

      assertEquals(project, projectService.findByIdentifier(project.identifier))
    }

  @Test
  fun `create saves a project with the requested name and a new identifier`() =
    runTest {
      coEvery { projectRepository.save(any()) } answers { firstArg() }

      val first = projectService.create(ProjectCreateRequest(name = "Website"))
      val second = projectService.create(ProjectCreateRequest(name = "Website"))

      assertEquals("Website", first.name)
      assertNotEquals(first.identifier, second.identifier)
      coVerify(exactly = 2) { projectRepository.save(any()) }
    }
}
