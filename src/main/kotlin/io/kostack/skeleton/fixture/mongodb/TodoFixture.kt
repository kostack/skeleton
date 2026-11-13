package io.kostack.skeleton.fixture.mongodb

import io.github.kostack.fixtures.AbstractFixture
import io.kostack.skeleton.controller.dto.todo.request.TodoCreateRequest
import io.kostack.skeleton.db.r2dbc.model.User
import io.kostack.skeleton.service.TodoService
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(2)
class TodoFixture(
  val service: TodoService
) : AbstractFixture() {
  override suspend fun load() {
    fixture1()
    fixture2()
    fixture3()
  }

  suspend fun fixture1() {
    val user = referenceRepository.getReference<User>("user_1")
    val todoCreateRequest = TodoCreateRequest(title = faker.word().noun())
    service.create(todoCreateRequest, user)
    this.referenceRepository.setReference("todo_1", todoCreateRequest)
  }

  suspend fun fixture2() {
    val user = referenceRepository.getReference<User>("user_1")
    val todoCreateRequest = TodoCreateRequest(title = faker.word().noun())
    service.create(todoCreateRequest, user)
    this.referenceRepository.setReference("todo_2", todoCreateRequest)
  }

  suspend fun fixture3() {
    val user = referenceRepository.getReference<User>("user_1")
    val todoCreateRequest = TodoCreateRequest(title = faker.word().noun())
    service.create(todoCreateRequest, user)
    this.referenceRepository.setReference("todo_3", todoCreateRequest)
  }
}
