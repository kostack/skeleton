package io.kostack.skeleton.command

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.shell.core.command.annotation.Command
import org.springframework.shell.core.command.annotation.Option
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
    @Option(defaultValue = "") arg: String
  ) {
    log.info("Test command called with argument: $arg")
    streamBridge.send("supplier-out-0", "hello from spring-cloud-stream")
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
