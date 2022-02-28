plugins {
  id("base-conventions")
  id("net.kyori.indra.publishing")
}

indra {
  github("jpenilla", "reflection-remapper")
  apache2License()
  configurePublications {
    pom {
      developers {
        developer {
          id.set("jmp")
          name.set("Jason Penilla")
          timezone.set("America/Phoenix")
        }
      }
    }
  }
}

signing {
  val signingKey: String? by project
  val signingPassword: String? by project
  useInMemoryPgpKeys(signingKey, signingPassword)
}
