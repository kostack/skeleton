plugins {
  id("io.github.kostack.spring-service")
}

group = "io.github.kostack"
description = "Skeleton project for Spring Boot"

kostack {
  coverageMinimum = 0.7
  coverageExclusions.add("**/generated/**")
  coverageExclusions.addAll("**/skeleton/Application*.class")
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
}
