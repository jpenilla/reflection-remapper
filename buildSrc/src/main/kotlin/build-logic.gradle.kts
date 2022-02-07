plugins {
  id("base-conventions")
  id("publishing-conventions")
  id("net.kyori.indra.publishing.sonatype")
}

val devBundle: Configuration by configurations.creating
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
  withType<Test> {
    if (!name.contains("Java8")) {
      inputs.files(memoryMeasurer)
      doFirst {
        jvmArgs("-javaagent:${memoryMeasurer.asFileTree.singleFile.path}")
      }
    }
    dependsOn(extractMappings)
  }
}

indraSonatype {
  useAlternateSonatypeOSSHost("s01")
}
