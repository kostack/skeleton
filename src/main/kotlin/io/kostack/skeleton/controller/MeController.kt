package io.kostack.skeleton.controller

import io.kostack.skeleton.db.r2dbc.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Me", description = "Me operations")
@RequestMapping("/api/{version}")
@RestController
class MeController {
  @Operation(
    operationId = "meAction",
    summary = "Retrieve current user"
  )
  @GetMapping("/me", version = "1", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("@defaultAuth.canView(authentication.principal)")
  suspend fun meAction(
    @AuthenticationPrincipal principal: User
  ): String = principal.identity

  @GetMapping("/me", version = "2+", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("@defaultAuth.canView(authentication.principal)")
  suspend fun meActionV2(
    @AuthenticationPrincipal principal: User
  ): User = principal
}
