package io.kostack.skeleton.fixture.r2dbc

import io.github.kostack.fixtures.AbstractFixture
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Order(2)
class ProjectFixture(
  private val projectRepository: ProjectRepository
) : AbstractFixture() {
  override suspend fun load() {
    createProject(1)
    createProject(2)
  }

  suspend fun createProject(idx: Int) {
    val project =
      projectRepository.save(
        Project(identifier = UUID.randomUUID().toString(), name = "${faker.word().noun()} $idx")
      )
    this.referenceRepository.setReference("project_$idx", project)
  }
}
