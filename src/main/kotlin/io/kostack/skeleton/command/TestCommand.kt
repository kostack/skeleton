package io.kostack.skeleton.command

import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.shell.core.command.annotation.Argument
import org.springframework.shell.core.command.annotation.Command
import org.springframework.stereotype.Component

@Component
class TestCommand(
  val streamBridge: StreamBridge
) {
  @Command(
    name = ["test"],
    description = "Test command"
  )
  fun test(
    @Argument(index = 0, description = "Free-text argument echoed back") arg: String?
  ) {
    val sent = streamBridge.send("supplier-out-0", "hello from spring-cloud-stream")
    println("Test command called with argument: ${arg.orEmpty()} (message sent: $sent)")
  }
}
