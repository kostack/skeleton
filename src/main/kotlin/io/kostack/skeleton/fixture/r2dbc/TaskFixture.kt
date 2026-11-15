package io.kostack.skeleton.fixture.r2dbc

import io.github.kostack.fixtures.AbstractFixture
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Task
import io.kostack.skeleton.db.r2dbc.model.TaskStatus
import io.kostack.skeleton.db.r2dbc.repository.ProjectRepository
import io.kostack.skeleton.db.r2dbc.repository.TaskRepository
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Order(3)
class TaskFixture(
  private val taskRepository: TaskRepository,
  private val projectRepository: ProjectRepository
) : AbstractFixture() {
  override suspend fun load() {
    val project1 = referenceRepository.getReference<Project>("project_1")
    val project2 = referenceRepository.getReference<Project>("project_2")
    val user1 = referenceRepository.getReference<User>("user_1")
    val user2 = referenceRepository.getReference<User>("user_2")

    val task1 = createTask(1, project1, TaskStatus.TODO, user1)
    createTask(2, project1, TaskStatus.IN_PROGRESS, user2)
    createTask(3, project1, TaskStatus.DONE, user1)
    createTask(4, project1, TaskStatus.TODO, null)
    createTask(5, project2, TaskStatus.TODO, user1)

    projectRepository.save(project1.copy(featuredTaskId = requireNotNull(task1.id)))
  }

  suspend fun createTask(
    idx: Int,
    project: Project,
    status: TaskStatus,
    assignee: User?
  ): Task {
    val task =
      taskRepository.save(
        Task(
          identifier = UUID.randomUUID().toString(),
          projectId = requireNotNull(project.id),
          title = "${faker.word().noun()} $idx",
          status = status,
          assigneeId = assignee?.identity
        )
      )
    this.referenceRepository.setReference("task_$idx", task)
    return task
  }
}
