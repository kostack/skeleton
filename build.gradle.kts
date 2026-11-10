import io.github.cdimascio.dotenv.Dotenv
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

buildscript {
  dependencies {
    classpath("io.github.cdimascio:dotenv-java:3.2.0")
  }
}

val springProfile =
  providers
    .gradleProperty("springProfile")
    .orElse(providers.environmentVariable("SPRING_PROFILES_ACTIVE"))
    .getOrElse("dev")

val envFilename =
  if (file("docker/.env.$springProfile").isFile) {
    ".env.$springProfile"
  } else {
    ".env"
  }

val dotenv: Dotenv =
  Dotenv
    .configure()
    .directory(file("docker").absolutePath)
    .filename(envFilename)
    .ignoreIfMissing()
    .load()

val applicationEnvironment =
  dotenv.entries().associate { it.key to it.value } +
    ("SPRING_PROFILES_ACTIVE" to springProfile)

plugins {
  // Kotlin plugin versions are managed in settings.gradle.kts
  kotlin("jvm")
  kotlin("plugin.spring")
  kotlin("plugin.serialization")
  id("org.springframework.boot") version "4.1.2"
  id("io.spring.dependency-management") version "1.1.7"
  id("com.diffplug.spotless") version "8.11.0"
  jacoco
}

group = "io.kostack"
version = "1.0.0"
description = "Skeleton project for Spring Boot"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xjsr305=strict")
  }
}

repositories {
  mavenCentral()
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.4")
    mavenBom("org.springframework.shell:spring-shell-dependencies:4.0.3")
  }
}
dependencies {
  // Kotlin
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("tools.jackson.module:jackson-module-kotlin")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.cloud:spring-cloud-starter-stream-rabbit")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
  implementation("org.springframework.shell:spring-shell-starter")

  // R2DBC
  implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
  runtimeOnly("io.asyncer:r2dbc-mysql")

  // Flyway for database migration
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.flywaydb:flyway-mysql")

  // JDBC (required by Flyway) and MySQL driver
  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  runtimeOnly("com.mysql:mysql-connector-j")

  // open api code generation
  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:3.2.0")

  implementation("com.github.pukkaone:logback-gelf:1.1.11")

  // kostack
  implementation("io.github.kostack:event-dispatcher:1.2.3")
  implementation("io.github.kostack:api-errors:1.0.0")
  implementation("io.github.kostack:database-tools:1.0.4")
  implementation("io.github.kostack:fixtures:1.1.0")
  implementation("io.github.kostack:jwt-security:2.0.0-beta-8")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.awaitility:awaitility:4.3.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
  testImplementation("com.ninja-squad:springmockk:5.0.1")
  testImplementation("io.mockk:mockk:1.14.11")
}

spotless {
  kotlin {
    target("src/**/*.kt")
    ktlint()
  }
  kotlinGradle {
    target("*.kts")
    ktlint()
  }
}

// Asks git for the hooks directory so worktrees and core.hooksPath are respected
val gitHooksDir =
  providers
    .exec {
      commandLine("git", "rev-parse", "--path-format=absolute", "--git-path", "hooks")
      workingDir = layout.projectDirectory.asFile
    }.standardOutput.asText
    .map { it.trim() }

tasks.register<Copy>("installGitHook") {
  group = "build setup"
  description = "Installs a git pre-commit hook to enforce formatting rules"

  val hookScript = layout.projectDirectory.file("scripts/pre-commit.sh")
  inputs.file(hookScript)

  from(hookScript) { rename { "pre-commit" } }
  into(gitHooksDir)
  filePermissions { unix("rwxr-xr-x") }
}

tasks.named<BootJar>("bootJar") {
  archiveBaseName.set("app")
  archiveVersion.set("")
}

tasks.named<BootRun>("bootRun") {
  environment(applicationEnvironment)
}

tasks.register<Exec>("runJar") {
  description = "Builds and runs the executable application jar"
  group = "application"

  val bootJar = tasks.named<BootJar>("bootJar")
  dependsOn(bootJar)

  environment(applicationEnvironment)

  val javaLauncher = javaToolchains.launcherFor(java.toolchain)

  inputs.file(bootJar.flatMap { it.archiveFile })

  doFirst {
    executable =
      javaLauncher
        .get()
        .executablePath.asFile.absolutePath
    args(
      "--enable-native-access=ALL-UNNAMED",
      "-jar",
      bootJar
        .get()
        .archiveFile
        .get()
        .asFile.absolutePath
    )
  }

  standardInput = System.`in`
  standardOutput = System.out
  errorOutput = System.err
}

tasks.withType<Test> {
  useJUnitPlatform()
  environment(applicationEnvironment)
  testLogging {
    events("passed", "skipped", "failed")
    showStandardStreams = true
  }
  jvmArgs("-XX:+EnableDynamicAgentLoading", "--enable-native-access=ALL-UNNAMED")
}

val coverageExclusions =
  listOf(
    "**/command/**",
    "**/configuration/**",
    "**/fixture/**"
  )

tasks.named<JacocoReport>("jacocoTestReport") {
  dependsOn(tasks.named<Test>("test"))

  reports {
    xml.required.set(true)
    html.required.set(true)
    csv.required.set(false)
    html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
    xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
  }

  classDirectories.setFrom(
    classDirectories.files.map { directory ->
      fileTree(directory) { exclude(coverageExclusions) }
    }
  )
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
  classDirectories.setFrom(
    classDirectories.files.map { directory ->
      fileTree(directory) { exclude(coverageExclusions) }
    }
  )
  violationRules {
    rule {
      limit { minimum = "0.5".toBigDecimal() }
    }
  }
}

tasks.named("check") {
  dependsOn("spotlessCheck", "jacocoTestReport", "jacocoTestCoverageVerification")
}
