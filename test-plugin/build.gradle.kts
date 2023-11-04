plugins {
  `base-conventions`
  id("io.papermc.paperweight.userdev")
  id("xyz.jpenilla.run-paper") version "2.2.0"
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

indra {
  javaVersions {
    target(17)
  }
}

dependencies {
  paperweight.paperDevBundle("1.20.2-R0.1-SNAPSHOT")

  implementation(projects.reflectionRemapper)

  implementation(platform("cloud.commandframework:cloud-bom:1.8.4"))
  implementation("cloud.commandframework", "cloud-paper")
  implementation("cloud.commandframework", "cloud-minecraft-extras") {
    isTransitive = false // Paper provides adventure
  }
}

tasks {
  assemble {
    dependsOn(reobfJar)
  }
  shadowJar {
    fun reloc(pkg: String) = relocate(pkg, "xyz.jpenilla.reflectionremapper.testplugin.dependency.$pkg")
    reloc("net.fabricmc.mappingio")
    reloc("cloud.commandframework")
    reloc("io.leangen.geantyref")
  }
  processResources {
    val props = mapOf(
      "version" to project.version,
      "desc" to project.description,
    )
    inputs.properties(props)
    filesMatching("plugin.yml") {
      expand(props)
    }
  }
}
