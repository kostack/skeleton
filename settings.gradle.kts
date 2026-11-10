pluginManagement {
  val kotlinVersion = "2.4.21"

  plugins {
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
  }
}

rootProject.name = "skeleton"
