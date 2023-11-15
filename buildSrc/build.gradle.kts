plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation(libs.indra.common)
  implementation(libs.indra.licenser.spotless)
  implementation(libs.indra.publishing.sonatype)
  implementation(libs.paperweight.userdev)
}
