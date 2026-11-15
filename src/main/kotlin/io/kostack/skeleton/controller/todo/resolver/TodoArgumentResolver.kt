package io.kostack.skeleton.controller.todo.resolver

import io.github.kostack.spring_tools.controller.resolver.MethodArgumentResolver
import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.db.mongodb.model.User
import io.kostack.skeleton.db.r2dbc.model.Todo
import io.kostack.skeleton.db.r2dbc.repository.TodoRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class TodoArgumentResolver(
  private val todoRepository: TodoRepository
) : HandlerMethodArgumentResolver,
  MethodArgumentResolver {
  override fun supportsParameter(parameter: MethodParameter): Boolean =
    parameter.hasParameterAnnotation(ResolvedArgument::class.java) &&
      parameter.parameterType == Todo::class.java

  override fun resolveArgument(
    parameter: MethodParameter,
    bindingContext: BindingContext,
    exchange: ServerWebExchange
  ): Mono<Any> =
    mono {
      val identifier = getPathParameter<String>("todoId", exchange)
      val authentication = exchange.getPrincipal<Authentication>().awaitSingle()
      val user = authentication.principal as User
      return@mono todoRepository.findByIdentifierAndCreatedBy(identifier, user.identity)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
}
