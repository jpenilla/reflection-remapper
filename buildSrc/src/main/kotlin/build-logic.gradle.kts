import io.papermc.paperweight.attribute.DevBundleOutput

plugins {
  id("base-conventions")
  id("publishing-conventions")
  id("org.incendo.cloud-build-logic.publishing.root-project")
}

val devBundle = configurations.create("devBundle") {
  attributes {
    attribute(DevBundleOutput.ATTRIBUTE, objects.named(DevBundleOutput.ZIP))
  }
}

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
    dependsOn(extractMappings)
  }
  withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) {
      rename("LICENSE", "META-INF/LICENSE_${rootProject.name}")
    }
  }
}
