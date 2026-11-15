package io.kostack.skeleton.api.todo.authorization

import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Todo
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
