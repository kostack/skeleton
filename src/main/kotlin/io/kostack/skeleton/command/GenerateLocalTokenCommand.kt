package io.kostack.skeleton.command

import io.github.kostack.jwt_security.encoder.JwtEncoder
import io.kostack.skeleton.service.UserService
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.shell.core.command.annotation.Command
import org.springframework.shell.core.command.annotation.Option
import org.springframework.stereotype.Component

@Component
class GenerateLocalTokenCommand(
  val encoder: JwtEncoder,
  val userService: UserService
) {
  @Command(
    name = ["generate-token"],
    description = "Generates local token"
  )
  fun test(
    @Option(longName = "identity", shortName = 'u', required = true) identity: String,
    @Option(longName = "role", shortName = 'r', required = false) role: String = "ROLE_CUSTOMER"
  ) = runBlocking {
    val user = userService.findByIdentity(identity) ?: throw IllegalArgumentException("User not found")
    val token = encoder.encode(user, mapOf("roles" to listOf(role)))
    log.info("Token: $token")
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
