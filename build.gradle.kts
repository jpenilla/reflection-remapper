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
  implementation("net.fabricmc", "mapping-io", "0.4.2") {
    exclude("org.ow2.asm")
  }

  // testing dependencies
  testImplementation(memoryMeasurer("com.volkhart.memory", "measurer", "0.2-SNAPSHOT"))
  devBundle("io.papermc.paper:dev-bundle:1.19.4-R0.1-SNAPSHOT")
}
