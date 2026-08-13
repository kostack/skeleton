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

val dotenv: Dotenv =
  Dotenv
    .configure()
    .directory("docker")
    .ignoreIfMissing()
    .load()

plugins {
  kotlin("jvm") version "2.3.0"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("plugin.serialization") version "2.3.0"
  id("org.springframework.boot") version "4.1.0"
  id("io.spring.dependency-management") version "1.1.7"
  id("com.diffplug.spotless") version "8.4.0"
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

repositories {
  mavenCentral()
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.2")
  }
}
dependencies {
  // Kotlin
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
  implementation("org.jetbrains.kotlin:kotlin-reflect")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.cloud:spring-cloud-starter-stream-rabbit")
  implementation("org.springframework.boot:spring-boot-starter-logging")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

  // R2DBC
  implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
  implementation("io.asyncer:r2dbc-mysql")

  // Flyway for database migration
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.flywaydb:flyway-mysql:12.3.0")

  // JDBC (required by Flyway) and MySQL driver
  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  implementation("com.mysql:mysql-connector-j:9.7.0")

  implementation("org.springframework.shell:spring-shell-starter:3.4.0")

  // open api code generation
  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.0")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.41")

  implementation("org.springframework.amqp:spring-rabbit:4.1.0")

  implementation("com.github.pukkaone:logback-gelf:1.1.11")

  // kostack
  implementation("io.github.kostack:event-dispatcher:1.2.2")
  implementation("io.github.kostack:jwt-security:1.0.2")
  implementation("io.github.kostack:api-errors:1.0.0")
  implementation("io.github.kostack:database-tools:1.0.3")
  implementation("io.github.kostack:fixtures:1.0.2")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("io.projectreactor:reactor-test:3.8.0")
  testImplementation("org.awaitility:awaitility:4.2.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
  testImplementation("com.ninja-squad:springmockk:5.0.1")
  testImplementation("io.mockk:mockk:1.13.10")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
  }
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

tasks.register("installGitHook") {
  group = "build setup"
  description = "Installs a git pre-commit hook to enforce formatting rules"
  doLast {
    val hookSource = file("scripts/pre-commit")
    val hooksDir = file(".git/hooks")
    val hookTarget = hooksDir.resolve("pre-commit")
    check(hookSource.isFile) { "Git hook source not found: ${hookSource.path}" }
    check(hooksDir.isDirectory) { "Git hooks directory not found: ${hooksDir.path}" }

    if (!hookTarget.exists()) {
      hookSource.copyTo(hookTarget, overwrite = false)
    }
    hookTarget.setExecutable(true)
  }
}

tasks.named<BootJar>("bootJar") {
  archiveBaseName.set("app")
  archiveVersion.set("")
}

tasks.named<BootRun>("bootRun") {
  dotenv.entries().forEach { entry -> environment(entry.key, entry.value) }
}

tasks.register<Exec>("runJar") {
  description = "Builds and runs the executable application jar"
  group = "application"

  val bootJar = tasks.named<BootJar>("bootJar")
  dependsOn(bootJar)

  val envVars = dotenv.entries().associate { it.key to it.value }
  environment(envVars)

  val javaLauncher =
    javaToolchains.launcherFor {
      languageVersion = JavaLanguageVersion.of(25)
    }

  inputs.file(bootJar.flatMap { it.archiveFile })

  doFirst {
    executable =
      javaLauncher
        .get()
        .executablePath.asFile.absolutePath
    args(
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
  dotenv.entries().forEach { entry -> environment(entry.key, entry.value) }
  testLogging {
    events("passed", "skipped", "failed")
    showStandardStreams = true
  }
  jvmArgs("-XX:+EnableDynamicAgentLoading")
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
  dependsOn("jacocoTestCoverageVerification", "spotlessCheck")
}
