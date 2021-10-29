plugins {
  `base-conventions`
  id("io.papermc.paperweight.userdev")
  id("xyz.jpenilla.run-paper") version "1.0.4"
  id("net.minecrell.plugin-yml.bukkit") version "0.5.0"
  id("com.github.johnrengelman.shadow") version "7.1.0"
}

dependencies {
  paperDevBundle("1.17.1-R0.1-SNAPSHOT")

  implementation(projects.reflectionRemapper)

  implementation("cloud.commandframework", "cloud-paper", "1.5.0")
  implementation("cloud.commandframework", "cloud-minecraft-extras", "1.5.0") {
    isTransitive = false // Paper provides adventure
  }
}

tasks {
  build {
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
  apiVersion = "1.17"
  authors = listOf("jmp")
}
