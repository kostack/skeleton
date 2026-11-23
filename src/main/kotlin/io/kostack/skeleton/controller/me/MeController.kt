package io.kostack.skeleton.controller.me

import io.kostack.skeleton.controller.me.response.MeResponse
import io.kostack.skeleton.controller.me.response.toMeResponse
import io.kostack.skeleton.db.mongodb.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/{version}/me")
@Tag(name = "Me", description = "Api own operations")
class MeController {
  @Operation(
    operationId = "user_me_action",
    summary = "Retrieve current user"
  )
  @GetMapping(version = "1+", produces = [MediaType.APPLICATION_JSON_VALUE])
  suspend fun getAction(
    @AuthenticationPrincipal user: User
  ): MeResponse = user.toMeResponse()
}
