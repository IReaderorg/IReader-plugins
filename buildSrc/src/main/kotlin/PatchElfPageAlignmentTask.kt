import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Task to patch ELF page alignment in .so files for Android 15+ (API 35+) compatibility.
 *
 * Android 15+ enforces 16KB (16384 byte) page size. Native libraries compiled with
 * 4KB (4096 byte) page alignment fail with:
 *   "dlopen failed: program alignment (4096) cannot be smaller than system page size (16384)"
 *
 * This task finds all .so files in the input directory and patches their ELF PT_LOAD
 * segment p_align values from 4096 to 16384.
 */
abstract class PatchElfPageAlignmentTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDir: DirectoryProperty

    @get:Input
    abstract val targetPageSize: Property<Int>

    init {
        targetPageSize.convention(16384)
    }

    @TaskAction
    fun execute() {
        val dir = inputDir.get().asFile
        val pageSize = targetPageSize.get()
        var patchedCount = 0

        dir.walkTopDown()
            .filter { it.isFile && it.extension == "so" }
            .forEach { soFile ->
                if (patchElfPageAlignment(soFile, pageSize)) {
                    patchedCount++
                    logger.lifecycle("Patched page alignment to ${pageSize} bytes: ${soFile.name}")
                }
            }

        if (patchedCount > 0) {
            logger.lifecycle("Patched $patchedCount native libraries for ${pageSize}-byte page alignment")
        } else {
            logger.lifecycle("No .so files needed page alignment patching in ${dir.absolutePath}")
        }
    }

    /**
     * Patch ELF PT_LOAD segment p_align values in a .so file.
     *
     * ELF layout (little-endian, 64-bit):
     *   Offset 0x00: e_ident (16 bytes) — magic + class + data encoding
     *   Offset 0x20: e_phoff — program header table offset (8 bytes)
     *   Offset 0x36: e_phentsize — program header entry size (2 bytes)
     *   Offset 0x38: e_phnum — number of program header entries (2 bytes)
     *
     * Each program header entry (64-bit):
     *   Offset 0x00: p_type  (4 bytes)
     *   Offset 0x04: p_flags (4 bytes)
     *   Offset 0x08: p_offset (8 bytes)
     *   Offset 0x10: p_vaddr  (8 bytes)
     *   Offset 0x18: p_paddr  (8 bytes)
     *   Offset 0x20: p_filesz (8 bytes)
     *   Offset 0x28: p_memsz  (8 bytes)
     *   Offset 0x30: p_align  (8 bytes) <-- this is what we patch
     *
     * ELF layout (little-endian, 32-bit):
     *   Offset 0x00: e_ident (16 bytes)
     *   Offset 0x1C: e_phoff (4 bytes)
     *   Offset 0x2A: e_phentsize (2 bytes)
     *   Offset 0x2C: e_phnum (2 bytes)
     *
     * Each program header entry (32-bit):
     *   Offset 0x00: p_type  (4 bytes)
     *   Offset 0x04: p_offset (4 bytes)
     *   Offset 0x08: p_vaddr  (4 bytes)
     *   Offset 0x0C: p_paddr  (4 bytes)
     *   Offset 0x10: p_filesz (4 bytes)
     *   Offset 0x14: p_memsz  (4 bytes)
     *   Offset 0x18: p_flags  (4 bytes)
     *   Offset 0x1C: p_align  (4 bytes) <-- this is what we patch
     */
    private fun patchElfPageAlignment(file: File, newPageSize: Int): Boolean {
        try {
            RandomAccessFile(file, "rw").use { raf ->
                val buf = ByteArray(4)
                raf.readFully(buf)

                // Check ELF magic: 0x7F 'E' 'L' 'F'
                if (buf[0] != 0x7F.toByte() || buf[1] != 'E'.code.toByte() ||
                    buf[2] != 'L'.code.toByte() || buf[3] != 'F'.code.toByte()
                ) {
                    return false
                }

                // EI_CLASS at offset 4: 1 = 32-bit, 2 = 64-bit
                raf.seek(4)
                val elfClass = raf.readByte().toInt()

                // EI_DATA at offset 5: 1 = little-endian, 2 = big-endian
                raf.seek(5)
                val isLittleEndian = raf.readByte().toInt() == 1
                val byteOrder = if (isLittleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN

                val is64Bit = elfClass == 2
                val alignOffset: Int
                val alignSize: Int

                if (is64Bit) {
                    // 64-bit ELF
                    raf.seek(0x20)
                    val phoff = readLong(raf, byteOrder)
                    raf.seek(0x36)
                    val phentsize = readShort(raf, byteOrder).toInt() and 0xFFFF
                    raf.seek(0x38)
                    val phnum = readShort(raf, byteOrder).toInt() and 0xFFFF

                    var patched = false
                    for (i in 0 until phnum) {
                        val entryOffset = phoff + i * phentsize
                        raf.seek(entryOffset)
                        val pType = readInt(raf, byteOrder)

                        // PT_LOAD = 1
                        if (pType == 1) {
                            raf.seek(entryOffset + 0x30)
                            val currentAlign = readLong(raf, byteOrder).toInt()

                            // Only patch if alignment is smaller than target (typically 4096 -> 16384)
                            if (currentAlign in 1 until newPageSize) {
                                raf.seek(entryOffset + 0x30)
                                writeLong(raf, newPageSize.toLong(), byteOrder)
                                patched = true
                            }
                        }
                    }
                    return patched
                } else {
                    // 32-bit ELF
                    raf.seek(0x1C)
                    val phoff = readInt(raf, byteOrder).toLong() and 0xFFFFFFFFL
                    raf.seek(0x2A)
                    val phentsize = readShort(raf, byteOrder).toInt() and 0xFFFF
                    raf.seek(0x2C)
                    val phnum = readShort(raf, byteOrder).toInt() and 0xFFFF

                    var patched = false
                    for (i in 0 until phnum) {
                        val entryOffset = phoff + i * phentsize
                        raf.seek(entryOffset)
                        val pType = readInt(raf, byteOrder)

                        // PT_LOAD = 1
                        if (pType == 1) {
                            raf.seek(entryOffset + 0x1C)
                            val currentAlign = readInt(raf, byteOrder).toLong() and 0xFFFFFFFFL

                            if (currentAlign in 1 until newPageSize) {
                                raf.seek(entryOffset + 0x1C)
                                writeInt(raf, newPageSize, byteOrder)
                                patched = true
                            }
                        }
                    }
                    return patched
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to patch ELF page alignment in ${file.name}: ${e.message}")
            return false
        }
    }

    private fun readShort(raf: RandomAccessFile, order: ByteOrder): Short {
        val buf = ByteArray(2)
        raf.readFully(buf)
        return ByteBuffer.wrap(buf).order(order).short
    }

    private fun readInt(raf: RandomAccessFile, order: ByteOrder): Int {
        val buf = ByteArray(4)
        raf.readFully(buf)
        return ByteBuffer.wrap(buf).order(order).int
    }

    private fun readLong(raf: RandomAccessFile, order: ByteOrder): Long {
        val buf = ByteArray(8)
        raf.readFully(buf)
        return ByteBuffer.wrap(buf).order(order).long
    }

    private fun writeInt(raf: RandomAccessFile, value: Int, order: ByteOrder) {
        val buf = ByteBuffer.allocate(4).order(order).putInt(value).array()
        raf.write(buf)
    }

    private fun writeLong(raf: RandomAccessFile, value: Long, order: ByteOrder) {
        val buf = ByteBuffer.allocate(8).order(order).putLong(value).array()
        raf.write(buf)
    }
}
