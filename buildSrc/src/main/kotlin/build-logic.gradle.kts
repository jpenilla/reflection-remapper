import io.papermc.paperweight.attribute.DevBundleOutput

plugins {
  id("base-conventions")
  id("publishing-conventions")
  id("net.kyori.indra.publishing.sonatype")
}

val devBundle: Configuration by configurations.creating {
  attributes {
    attribute(DevBundleOutput.ATTRIBUTE, objects.named(DevBundleOutput.ZIP))
  }
}
val memoryMeasurer: Configuration by configurations.creating

tasks {
  val mappingsDir = layout.buildDirectory.dir("test_mappings")
  val extractMappings = register<ExtractMappings>("extractMappings") {
    devBundleZip.set(layout.file(devBundle.elements.map { it.single().asFile }))
    out.set(mappingsDir.map { it.file("mappings.tiny") })
  }
  sourceSets.test {
    resources.srcDir(mappingsDir)
  }
  processTestResources {
    dependsOn(extractMappings)
  }
  withType<Test>().configureEach {
    if (!name.contains("Java8")) {
      inputs.files(memoryMeasurer)
      doFirst {
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
        jvmArgs("-javaagent:${memoryMeasurer.asFileTree.singleFile.path}")
      }
    }
    dependsOn(extractMappings)
  }
}

indraSonatype {
  useAlternateSonatypeOSSHost("s01")
}
