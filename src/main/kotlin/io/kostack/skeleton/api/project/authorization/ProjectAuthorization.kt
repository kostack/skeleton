package io.kostack.skeleton.api.project.authorization

import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Project
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component("projectAuth")
class ProjectAuthorization {
  fun requireOwner(
    project: Project,
    principal: User
  ): Mono<Boolean> =
    mono {
      project.ownerId == principal.identity
    }
}
