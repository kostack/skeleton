package io.kostack.skeleton.command

import io.github.kostack.jwt_security.encoder.JwtEncoder
import io.kostack.skeleton.service.UserService
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile
import org.springframework.shell.core.command.annotation.Command
import org.springframework.shell.core.command.annotation.Option
import org.springframework.stereotype.Component

@Profile("shell")
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
    @Option(longName = "identity", shortName = 'u', required = true) identity: String
  ) = runBlocking {
    val user = userService.findByIdentity(identity) ?: throw IllegalArgumentException("User not found")
    val token = encoder.encode(user)
    println(token)
  }
}
