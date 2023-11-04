plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  implementation("net.kyori", "indra-common", "3.1.3")
  implementation("net.kyori", "indra-licenser-spotless", "3.1.3")
  implementation("net.kyori", "indra-publishing-sonatype", "3.1.3")
  implementation("io.papermc.paperweight.userdev", "io.papermc.paperweight.userdev.gradle.plugin", "1.5.9")
}
