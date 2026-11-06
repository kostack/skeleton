package io.kostack.skeleton.controller

import io.kostack.skeleton.db.r2dbc.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Api", description = "Api operations")
@RestController
class ApiController {
  @Operation(
    operationId = "meAction",
    summary = "Retrieve current user"
  )
  @GetMapping("/api/me", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("@defaultAuth.canView(authentication.principal)")
  suspend fun meAction(
    @AuthenticationPrincipal principal: User
  ): User = principal
}
