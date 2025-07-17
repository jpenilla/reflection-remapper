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
  implementation(libs.paperweight.userdev)
  implementation(libs.cloud.build.logic)
}
