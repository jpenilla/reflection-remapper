plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  mavenCentral()
  maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
  implementation("net.kyori", "indra-common", "2.1.1")
  implementation("net.kyori", "indra-publishing-sonatype", "2.1.1")
  implementation("gradle.plugin.org.cadixdev.gradle", "licenser", "0.6.1")
  implementation("io.papermc.paperweight.userdev", "io.papermc.paperweight.userdev.gradle.plugin", "1.3.5")
}
