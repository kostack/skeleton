plugins {
  id("io.github.kostack.spring-service")
}

group = "io.github.kostack"
description = "Skeleton project for Spring Boot"

kostack {
  coverageMinimum = 0.7
  coverageExclusions.add("**/generated/**")
}

// API docs UI: Scalar instead of Swagger UI (the kostack conventions add the Swagger UI starter)
configurations.configureEach {
  exclude(group = "org.springdoc", module = "springdoc-openapi-starter-webflux-ui")
}

dependencies {
  implementation("org.springdoc:springdoc-openapi-starter-webflux-scalar:3.2.0")

  // Newer than the version shipped with the kostack conventions (Pagination items reference their schema)
  implementation("io.github.kostack:database-tools:1.1.1")

  // Spring Shell: JLine line reader (Tab completion, history, line editing)
  implementation("org.springframework.shell:spring-shell-jline")

  // MongoDB
  implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

  // R2DBC
  implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
  runtimeOnly("io.asyncer:r2dbc-mysql")

  // Flyway for database migration
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.flywaydb:flyway-mysql")

  // JDBC (required by Flyway) and MySQL driver
  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  runtimeOnly("com.mysql:mysql-connector-j")
}

// JLine (Spring Shell) uses the FFM API; grant native access to avoid the restricted-method warning.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
  jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
  manifest {
    attributes("Enable-Native-Access" to "ALL-UNNAMED")
  }
}
