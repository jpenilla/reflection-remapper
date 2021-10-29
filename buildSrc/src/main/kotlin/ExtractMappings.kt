import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files

@CacheableTask
abstract class ExtractMappings : DefaultTask() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val devBundleZip: RegularFileProperty

  @get:OutputFile
  abstract val out: RegularFileProperty

  @TaskAction
  fun run() {
    out.get().asFile.parentFile.mkdirs()
    Files.deleteIfExists(out.get().asFile.toPath())
    val bundleZip = devBundleZip.get().asFile
    FileSystems.newFileSystem(URI.create("jar:${bundleZip.toURI()}"), HashMap<String, Any>()).use { fs ->
      Files.copy(fs.getPath("/data/mojang+yarn-spigot-reobf.tiny"), out.get().asFile.toPath())
    }
  }
}
