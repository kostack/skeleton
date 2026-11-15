package io.kostack.skeleton.fixture.r2dbc

import io.github.kostack.fixtures.AbstractFixture
import io.kostack.skeleton.db.mongodb.model.User
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
    createProject(1, referenceRepository.getReference<User>("user_1"))
    createProject(2, referenceRepository.getReference<User>("user_2"))
    createProject(3, referenceRepository.getReference<User>("user_1"))
  }

  suspend fun createProject(
    idx: Int,
    owner: User
  ) {
    val project =
      projectRepository.save(
        Project(
          identifier = UUID.randomUUID().toString(),
          name = "${faker.word().noun()} $idx",
          description = "Description of project $idx",
          ownerId = owner.identity
        )
      )
    this.referenceRepository.setReference("project_$idx", project)
  }
}
