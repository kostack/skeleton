package io.kostack.skeleton.controller.user.operation

import io.kostack.skeleton.db.mongodb.model.User
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping

@UserOperation
class GetMe {
  @Operation(
    operationId = "user_me_action",
    summary = "Retrieve current user"
  )
  @GetMapping("/me", version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend operator fun invoke(
    @AuthenticationPrincipal user: User
  ): User = user
}
