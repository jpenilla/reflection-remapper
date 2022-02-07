plugins {
  id("base-conventions")
  id("net.kyori.indra.publishing")
  id("net.kyori.indra.publishing.sonatype")
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

indraSonatype {
  useAlternateSonatypeOSSHost("s01")
}

signing {
  useInMemoryPgpKeys(
    providers.gradleProperty("signingKey").forUseAtConfigurationTime().orNull,
    providers.gradleProperty("signingPassword").forUseAtConfigurationTime().orNull
  )
}
