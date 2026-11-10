pluginManagement {
  val kotlinVersion = "2.4.21"

  plugins {
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
  }
}

rootProject.name = "skeleton"

// Project coordinates, applied to the root project
gradle.rootProject {
  group = "io.github.kostack"
  description = "Skeleton project for Spring Boot"
}

// JaCoCo coverage settings, read in build.gradle.kts
gradle.rootProject {
  extra["coverageMinimum"] = "0.5".toBigDecimal()

  extra["coverageExclusions"] =
    listOf(
      "**/command/**",
      "**/configuration/**",
      "**/fixture/**"
    )
}
