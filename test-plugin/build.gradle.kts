plugins {
  `base-conventions`
  id("io.papermc.paperweight.userdev")
  id("xyz.jpenilla.run-paper") version "2.0.1"
  id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

indra {
  javaVersions {
    target(17)
  }
}

dependencies {
  paperDevBundle("1.19.3-R0.1-SNAPSHOT")

  implementation(projects.reflectionRemapper)

  implementation(platform("cloud.commandframework:cloud-bom:1.8.0"))
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
}

bukkit {
  name = "ReflectionRemapperTest"
  main = "xyz.jpenilla.reflectionremapper.testplugin.TestPlugin"
  apiVersion = "1.19"
  authors = listOf("jmp")
}
