package io.kostack.skeleton.configuration

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Spring Shell runs only with the shell profile. Outside it, interactive mode is off, and Spring Shell
 * would fall back to its non-interactive runner, which logs an error when no command is passed.
 * This bean replaces that runner (Spring Shell backs off when a bean with this name exists).
 */
@Configuration
@Profile("!shell")
class ShellRunnerConfig {
  @Bean
  fun springShellApplicationRunner(): ApplicationRunner = ApplicationRunner { }
}
