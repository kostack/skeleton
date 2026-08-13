package io.kostack.skeleton.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import jakarta.validation.Valid
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.method.HandlerMethod
import java.util.*

@Configuration
class OpenApiConfiguration : OpenApiCustomizer {
  override fun customise(openApi: OpenAPI) {
    val components = openApi.components
    components.responses = ApiResponses()

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
  }

  @Bean
  fun customizeOperations(): OperationCustomizer =
    OperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
      // Remove 400 response for methods without @Valid or @RequestBody
      val hasValidation =
        Arrays
          .stream(handlerMethod.methodParameters)
          .anyMatch { param: MethodParameter? ->
            param!!.hasParameterAnnotation(Valid::class.java) ||
              param.hasParameterAnnotation(RequestBody::class.java)
          }

      if (!hasValidation && operation.responses != null) {
        operation.responses.remove("400")
      }
      operation
    }
}
