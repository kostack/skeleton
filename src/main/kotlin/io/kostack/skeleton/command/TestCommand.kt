package io.kostack.skeleton.command

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
class TestCommand(
  val streamBridge: StreamBridge
) {
  @ShellMethod(key = ["test"], value = "Test command")
  fun test(
    @ShellOption(defaultValue = "") arg: String
  ) {
    log.info("Test command called with argument: $arg")
    streamBridge.send("supplier-out-0", "hello from spring-cloud-stream")
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
