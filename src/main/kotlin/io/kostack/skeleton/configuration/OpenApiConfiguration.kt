package io.kostack.skeleton.configuration

import io.github.kostack.spring_tools.controller.resolver.ResolvedArgument
import io.github.kostack.spring_tools.dto.ApiValidation
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import jakarta.validation.Valid
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.method.HandlerMethod

@Configuration
class OpenApiConfiguration : OpenApiCustomizer {
  companion object {
    private const val BAD_REQUEST = "BadRequest"
    private const val UNAUTHORIZED = "Unauthorized"
    private const val FORBIDDEN = "Forbidden"
    private const val NOT_FOUND = "NotFound"
  }

  override fun customise(openApi: OpenAPI) {
    val components = openApi.components

    ModelConverters
      .getInstance()
      .readAll(ApiValidation::class.java)
      .forEach { (name, schema) -> components.addSchemas(name, schema) }

    components.responses =
      ApiResponses()
        .addApiResponse(
          BAD_REQUEST,
          ApiResponse()
            .description("Invalid request parameters or body")
            .content(
              Content().addMediaType(
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                MediaType().schema(Schema<ApiValidation>().`$ref`(ApiValidation::class.java.simpleName))
              )
            )
        ).addApiResponse(UNAUTHORIZED, ApiResponse().description("Missing or invalid access token"))
        .addApiResponse(FORBIDDEN, ApiResponse().description("Not allowed to access this resource"))
        .addApiResponse(NOT_FOUND, ApiResponse().description("Resource not found"))

    components
      .addSecuritySchemes(
        "bearer-key",
        SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
      )

    openApi
      .components(components)
      .addSecurityItem(SecurityRequirement().addList("bearer-key"))
      .info(Info().title("API Documentation").description("Restricted API documentation."))

    openApi.paths = prefixApiVersions(openApi.paths)
  }

  private fun prefixApiVersions(paths: Paths?): Paths? {
    if (paths == null) return null

    val index = WebFluxConfiguration.API_VERSION_PATH_SEGMENT + 1 // leading "/" yields an empty first element
    return Paths().apply {
      paths.forEach { (path, item) ->
        val segments = path.split("/").toMutableList()
        if (path.startsWith(WebFluxConfiguration.API_PATH_PREFIX) &&
          segments.getOrNull(index)?.firstOrNull()?.isDigit() == true
        ) {
          segments[index] = "v${segments[index]}"
        }
        addPathItem(segments.joinToString("/"), item)
      }
    }
  }

  /**
   * Adds the common error responses each operation can actually return.
   * Responses declared explicitly on an operation take precedence.
   */
  @Bean
  fun customizeOperations(): OperationCustomizer =
    OperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
      val parameters = handlerMethod.methodParameters
      val responses = operation.responses ?: ApiResponses().also { operation.responses = it }

      fun addRef(
        code: String,
        name: String
      ) = responses.putIfAbsent(code, ApiResponse().`$ref`(name))

      val validated =
        parameters.any { param: MethodParameter ->
          param.hasParameterAnnotation(Valid::class.java) ||
            param.hasParameterAnnotation(RequestBody::class.java)
        }
      val authorized =
        handlerMethod.hasMethodAnnotation(PreAuthorize::class.java) ||
          handlerMethod.beanType.isAnnotationPresent(PreAuthorize::class.java)
      val resolvesResource = parameters.any { it.hasParameterAnnotation(ResolvedArgument::class.java) }

      if (validated) addRef("400", BAD_REQUEST) else responses.remove("400")
      addRef("401", UNAUTHORIZED)
      if (authorized) addRef("403", FORBIDDEN)
      if (resolvesResource) addRef("404", NOT_FOUND)

      operation
    }
}
