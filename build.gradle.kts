plugins {
  id("io.github.kostack.spring-service")
}

group = "io.github.kostack"
description = "Skeleton project for Spring Boot"

kostack {
  coverageMinimum = 0.8
  coverageExclusions.add("**/generated/**")
}

dependencies {

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

  // Testcontainers: MySQL, MongoDB and RabbitMQ for integration tests, wired via @ServiceConnection
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("org.testcontainers:testcontainers-junit-jupiter")
  testImplementation("org.testcontainers:testcontainers-mysql")
  testImplementation("org.testcontainers:testcontainers-r2dbc")
  testImplementation("org.testcontainers:testcontainers-mongodb")
  testImplementation("org.testcontainers:testcontainers-rabbitmq")
}
