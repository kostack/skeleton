package io.kostack.skeleton.fixture.r2dbc

import io.github.kostack.fixtures.AbstractFixture
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Order(3)
class TodoFixture(
  private val todoRepository: TodoRepository
) : AbstractFixture() {
  override suspend fun load() {
    val project1 = referenceRepository.getReference<Project>("project_1")
    val project2 = referenceRepository.getReference<Project>("project_2")
    createTodo(1, referenceRepository.getReference<User>("user_1"), project1)
    createTodo(2, referenceRepository.getReference<User>("user_1"), project1)
    createTodo(3, referenceRepository.getReference<User>("user_1"), project2, true)
    createTodo(4, referenceRepository.getReference<User>("user_2"), project1)
    createTodo(5, referenceRepository.getReference<User>("user_2"), project2, true)
  }

  suspend fun createTodo(
    idx: Int,
    user: User,
    project: Project,
    completed: Boolean = false
  ) {
    val todo =
      todoRepository.save(
        Todo(
          identifier = UUID.randomUUID().toString(),
          title = "${faker.word().noun()} $idx",
          completed = completed,
          projectId = requireNotNull(project.id),
          createdBy = user.identity
        )
      )
    this.referenceRepository.setReference("todo_$idx", todo)
  }
}
