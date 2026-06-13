import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class PatchJ2V8PageSizeTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val jniLibsDir: DirectoryProperty

    @get:Input
    abstract val cacheDir: Property<String>

    @get:Input
    abstract val patcherScript: Property<String>

    @TaskAction
    fun execute() {
        val dir = jniLibsDir.get().asFile
        val cache = File(cacheDir.get())

        val script = File(patcherScript.get())
        if (!script.exists()) {
            logger.warn("LIEF patcher script not found at ${script.absolutePath}, skipping")
            return
        }

        val pythonExec = findPython() ?: run {
            logger.warn("Python not found, skipping J2V8 page size patching")
            return
        }

        val liefCheck = ProcessBuilder(pythonExec, "-c", "import lief; print('ok')")
            .redirectErrorStream(true).start()
        val liefOk = liefCheck.inputStream.bufferedReader().readText().trim()
        liefCheck.waitFor()
        if (liefOk != "ok") {
            logger.warn("LIEF not installed (pip install lief), skipping")
            return
        }

        logger.lifecycle("Patching J2V8 native libraries for 16KB page alignment...")

        val process = ProcessBuilder(pythonExec, script.absolutePath, dir.absolutePath, cache.absolutePath)
            .redirectErrorStream(true).start()

        process.inputStream.bufferedReader().readLines().filter { it.isNotBlank() }.forEach { logger.lifecycle("  $it") }
        val exitCode = process.waitFor()
        if (exitCode != 0) logger.warn("LIEF patcher exited with code $exitCode")
        else logger.lifecycle("J2V8 native libraries patched for 16KB page alignment")
    }

    private fun findPython(): String? {
        for (cmd in listOf("python3", "python")) {
            try {
                val p = ProcessBuilder(cmd, "--version").start()
                p.waitFor()
                if (p.exitValue() == 0) return cmd
            } catch (_: Exception) {}
        }
        return null
    }
}
