plugins {
  id("base-conventions")
  id("net.kyori.indra.publishing")
}

indra {
  publishSnapshotsTo("jmp", "https://repo.jpenilla.xyz/snapshots/")
  github("jpenilla", "reflection-remapper")
  apache2License()
  configurePublications {
    pom {
      developers {
        developer {
          id.set("jmp")
          name.set("Jason Penilla")
          timezone.set("America/Los Angeles")
        }
      }
    }
  }
}
