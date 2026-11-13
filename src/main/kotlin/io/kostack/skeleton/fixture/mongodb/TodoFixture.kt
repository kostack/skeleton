package io.kostack.skeleton.fixture.mongodb

import io.github.kostack.fixtures.AbstractFixture
import io.kostack.skeleton.db.mongodb.model.Todo
import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.service.todo.TodoService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Order(2)
class TodoFixture(
  val service: TodoService
) : AbstractFixture() {
  override suspend fun load() {
    createTodo(1, referenceRepository.getReference<User>("user_1"))
    createTodo(2, referenceRepository.getReference<User>("user_1"))
    createTodo(3, referenceRepository.getReference<User>("user_1"), true)
    createTodo(4, referenceRepository.getReference<User>("user_2"))
    createTodo(5, referenceRepository.getReference<User>("user_2"), true)
  }

  suspend fun createTodo(
    idx: Int,
    user: User,
    completed: Boolean = false
  ) {
    val todo =
      service.save(
        Todo(
          id = UUID.randomUUID().toString(),
          title = "${faker.word().noun()} $idx",
          completed = completed,
          createdBy = user.identity
        )
      )
    this.referenceRepository.setReference("todo_$idx", todo)
  }
}
