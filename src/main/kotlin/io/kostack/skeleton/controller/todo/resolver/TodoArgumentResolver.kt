package io.kostack.skeleton.controller.todo.resolver

import io.github.kostack.spring_tools.controller.resolver.MethodArgumentResolver
import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.kostack.skeleton.controller.todo.operation.GetTodo
import io.kostack.skeleton.db.r2dbc.model.Todo
import kotlinx.coroutines.reactor.mono
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class TodoArgumentResolver(
  private val getTodo: GetTodo
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
      return@mono getTodo(identifier)
    }
}
