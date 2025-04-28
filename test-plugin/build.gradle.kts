plugins {
  `base-conventions`
  id("io.papermc.paperweight.userdev")
  alias(libs.plugins.run.paper)
  alias(libs.plugins.shadow)
}

indra {
  javaVersions {
    target(21)
  }
}

dependencies {
  paperweight.paperDevBundle(libs.versions.testPluginDevBundle)

  implementation(projects.reflectionRemapper)

  implementation(platform(libs.cloud.bom))
  implementation(platform(libs.cloud.minecraft.bom))
  implementation(libs.cloud.paper)
  implementation(libs.cloud.minecraft.extras) {
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
    reloc("org.incendo.cloud")
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
