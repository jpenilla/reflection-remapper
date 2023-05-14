plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  implementation("net.kyori", "indra-common", "3.0.1")
  implementation("net.kyori", "indra-licenser-cadix", "3.0.1")
  implementation("net.kyori", "indra-publishing-sonatype", "3.0.1")
  implementation("gradle.plugin.org.cadixdev.gradle", "licenser", "0.6.1")
  implementation("io.papermc.paperweight.userdev", "io.papermc.paperweight.userdev.gradle.plugin", "1.5.5")
}
