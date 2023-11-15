plugins {
  `java-library`
  id("net.kyori.indra")
  id("net.kyori.indra.checkstyle")
  id("net.kyori.indra.licenser.spotless")
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

indraSpotlessLicenser {
  licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
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
      rename("LICENSE", "META-INF/LICENSE_${rootProject.name}")
    }
  }
}

repositories {
  mavenCentral()
}
