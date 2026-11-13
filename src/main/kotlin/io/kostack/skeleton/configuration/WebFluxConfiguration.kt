package io.kostack.skeleton.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver
import org.springframework.web.reactive.config.ApiVersionConfigurer
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer

@Configuration
class WebFluxConfiguration : WebFluxConfigurer {
  companion object {
    const val API_PATH_PREFIX = "/api/"

    /** Index of the version segment in /api/{version}/... */
    const val API_VERSION_PATH_SEGMENT = 1
  }

  override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
    configurer.addCustomResolver(ReactivePageableHandlerMethodArgumentResolver())
  }

  override fun configureApiVersioning(configurer: ApiVersionConfigurer) {
    configurer.usePathSegment(API_VERSION_PATH_SEGMENT) { path ->
      path.pathWithinApplication().value().startsWith(API_PATH_PREFIX)
    }
  }
}
