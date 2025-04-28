plugins {
  `build-logic`
  alias(libs.plugins.shadow)
}

group = "xyz.jpenilla"
version = "0.1.2"
description = "reflection-remapper is a library to simplify making reflective calls, with specific support for remapped environments."

repositories {
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  compileOnlyApi(libs.checker.qual)
  implementation(libs.mapping.io)

  // testing dependencies
  testImplementation(libs.memoryMeasurer)
  memoryMeasurer(libs.memoryMeasurer)
  devBundle(libs.mappingsTestDevBundle)
  testImplementation(libs.junit.jupiter.api)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.shadowJar {
  relocate("net.fabricmc.mappingio", "xyz.jpenilla.reflectionremapper.internal.lib.mappingio")
}
