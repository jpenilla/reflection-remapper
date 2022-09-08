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
  javaVersions {
    minimumToolchain(17)
    target(8)
    testWith(8, 17)
  }
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
  compileOnlyApi("org.checkerframework", "checker-qual", "3.24.0")

  testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.9.0")
  testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine")
}
