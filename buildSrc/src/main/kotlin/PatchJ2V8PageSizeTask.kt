import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task to patch J2V8 native libraries for Android 15+ (16KB page size) compatibility.
 *
 * Uses LIEF (Python library) to properly rewrite ELF segment layout:
 * 1. Sets p_align >= 16384 for all PT_LOAD segments
 * 2. Ensures (p_vaddr % 16384) == (p_offset % 16384) for all PT_LOAD segments
 *
 * Requires: pip install lief
 */
abstract class PatchJ2V8PageSizeTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val jniLibsDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val cacheDir: DirectoryProperty

    @get:Input
    abstract val patcherScriptPath: String

    @TaskAction
    fun execute() {
        val dir = jniLibsDir.get().asFile
        val cache = cacheDir.get().asFile

        val patcherScript = File(patcherScriptPath)
        if (!patcherScript.exists()) {
            logger.warn("LIEF patcher script not found at ${patcherScript.absolutePath}, skipping page size patching")
            return
        }

        val pythonExec = findPython()
        if (pythonExec == null) {
            logger.warn("Python not found, skipping J2V8 page size patching")
            return
        }

        val liefCheck = ProcessBuilder(pythonExec, "-c", "import lief; print('ok')")
            .redirectErrorStream(true)
            .start()
        val liefOutput = liefCheck.inputStream.bufferedReader().readText().trim()
        liefCheck.waitFor()
        if (liefOutput != "ok") {
            logger.warn("LIEF not installed (pip install lief), skipping J2V8 page size patching")
            return
        }

        logger.lifecycle("Patching J2V8 native libraries for 16KB page alignment using LIEF...")

        val process = ProcessBuilder(
            pythonExec,
            patcherScript.absolutePath,
            dir.absolutePath,
            cache.absolutePath
        )
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        output.lines().forEach { line ->
            if (line.isNotBlank()) {
                logger.lifecycle("  $line")
            }
        }

        if (exitCode != 0) {
            logger.warn("LIEF patcher exited with code $exitCode")
        } else {
            logger.lifecycle("J2V8 native libraries patched for 16KB page alignment")
        }
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
