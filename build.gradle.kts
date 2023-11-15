plugins {
  `build-logic`
}

group = "xyz.jpenilla"
version = "0.1.0-SNAPSHOT"
description = "reflection-remapper is a library to simplify making reflective calls, with specific support for remapped environments."

repositories {
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  compileOnlyApi(libs.checker.qual)
  implementation(libs.mapping.io) {
    exclude("org.ow2.asm")
  }

  // testing dependencies
  testImplementation(libs.memoryMeasurer)
  memoryMeasurer(libs.memoryMeasurer)
  devBundle(libs.mappingsTestDevBundle)
  testImplementation(libs.junit.jupiter.api)
  testRuntimeOnly(libs.junit.jupiter.engine)
}
