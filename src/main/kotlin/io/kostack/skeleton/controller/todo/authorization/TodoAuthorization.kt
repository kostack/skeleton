package io.kostack.skeleton.controller.todo.authorization

import io.kostack.skeleton.db.mongodb.model.Todo
import io.kostack.skeleton.db.r2dbc.model.User
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component("todoAuth")
class TodoAuthorization {
  fun requireOwner(
    todo: Todo,
    principal: User
  ): Mono<Boolean> =
    mono {
      todo.createdBy == principal.identity
    }
}
