import org.incendo.cloudbuildlogic.jmp

plugins {
  id("base-conventions")
  id("net.kyori.indra.publishing")
  id("org.incendo.cloud-build-logic.publishing")
}

indra {
  github("jpenilla", "reflection-remapper")
  apache2License()
  configurePublications {
    pom {
      developers {
        jmp()
      }
    }
  }
  signWithKeyFromProperties("signingKey", "signingPassword")
}
