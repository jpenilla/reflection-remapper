plugins {
  `java-library`
  id("net.kyori.indra")
  id("net.kyori.indra.checkstyle")
  id("net.kyori.indra.license-header")
}

if (project != rootProject) {
  group = rootProject.group
  version = rootProject.version
  description = rootProject.description
}

indra {
  javaVersions().target(16)
}

license {
  header.set(resources.text.fromFile(rootProject.file("LICENSE_HEADER")))
}

tasks {
  jar {
    manifest {
      attributes(
        "Automatic-Module-Name" to "xyz.jpenilla.reflectionremapper"
      )
    }
  }
  withType<Jar> {
    from(rootProject.file("LICENSE")) {
      rename { "LICENSE_${rootProject.name}" }
    }
  }
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnlyApi("org.checkerframework", "checker-qual", "3.18.1")

  testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.1")
  testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine")
}
