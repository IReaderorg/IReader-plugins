package io.github.ireaderorg.plugins.pipertts

import ireader.plugin.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path

/**
 * Piper TTS Plugin for Desktop.
 * 
 * Provides high-performance neural text-to-speech using Piper,
 * a fast local neural TTS system.
 * 
 * This plugin uses reflection to access Piper JNI classes at runtime,
 * allowing it to compile without platform-specific dependencies.
 * 
 * Features:
 * - 30+ voices in 20+ languages
 * - High-quality neural speech synthesis
 * - Fully offline after voice download
 * - ~20-25MB per platform (native libraries)
 * - ~15-50MB per voice model
 */
class PiperTTSPlugin : TTSPlugin {
    
    private var pluginContext: PluginContext? = null
    private var piperInstance: Any? = null
    private var currentVoice: Any? = null
    private var voicesDir: File? = null
    private var isAvailable = false
    
    // Cached reflection classes
    private var piperJNIClass: Class<*>? = null
    private var piperVoiceClass: Class<*>? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.piper-tts",
        name = "Piper TTS",
        version = "1.2.0",
        versionCode = 1,
        description = "High-performance neural text-to-speech with 30+ voices in 20+ languages",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.TTS,
        permissions = listOf(PluginPermission.STORAGE, PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.DESKTOP), // Desktop only!
        nativeLibraries = mapOf(
            "windows-x64" to listOf(
                "native/windows-x64/piper_jni.dll",
                "native/windows-x64/onnxruntime.dll",
                "native/windows-x64/espeak-ng.dll"
            ),
            "macos-x64" to listOf(
                "native/macos-x64/libpiper_jni.dylib",
                "native/macos-x64/libonnxruntime.dylib",
                "native/macos-x64/libespeak-ng.dylib"
            ),
            "macos-arm64" to listOf(
                "native/macos-arm64/libpiper_jni.dylib",
                "native/macos-arm64/libonnxruntime.dylib",
                "native/macos-arm64/libespeak-ng.dylib"
            ),
            "linux-x64" to listOf(
                "native/linux-x64/libpiper_jni.so",
                "native/linux-x64/libonnxruntime.so",
                "native/linux-x64/libespeak-ng.so"
            )
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        
        // Check platform - this plugin is Desktop-only
        if (context.getPlatform() != Platform.DESKTOP) {
            context.log(LogLevel.WARN, "Piper TTS is only available on Desktop")
            isAvailable = false
            return
        }
        
        // Setup voices directory
        voicesDir = File(context.getDataDir(), "voices")
        voicesDir?.mkdirs()
        
        // Try to load Piper JNI classes via reflection
        isAvailable = try {
            piperJNIClass = Class.forName("io.github.givimad.piperjni.PiperJNI")
            piperVoiceClass = Class.forName("io.github.givimad.piperjni.PiperVoice")
            
            // Create PiperJNI instance
            piperInstance = piperJNIClass!!.getDeclaredConstructor().newInstance()
            
            context.log(LogLevel.INFO, "Piper TTS initialized successfully")
            true
        } catch (e: ClassNotFoundException) {
            context.log(LogLevel.ERROR, "Piper JNI library not found - please ensure it's bundled with the app")
            false
        } catch (e: Exception) {
            context.log(LogLevel.ERROR, "Failed to initialize Piper TTS: ${e.message}")
            false
        }
    }
    
    override fun cleanup() {
        // Close current voice
        if (currentVoice != null && piperVoiceClass != null) {
            try {
                val closeMethod = piperVoiceClass!!.getMethod("close")
                closeMethod.invoke(currentVoice)
            } catch (e: Exception) {
                // Ignore
            }
        }
        currentVoice = null
        piperInstance = null
        pluginContext = null
        isAvailable = false
    }
    
    override suspend fun speak(text: String, voice: VoiceConfig): Result<AudioStream> {
        val piper = piperInstance ?: return Result.failure(
            IllegalStateException("Piper TTS not initialized")
        )
        
        return withContext(Dispatchers.IO) {
            try {
                // Load voice if needed
                val voiceModel = loadVoice(voice.voiceId)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Voice not found: ${voice.voiceId}")
                    )
                
                // Synthesize speech using reflection
                val textToAudioMethod = piperJNIClass!!.getMethod(
                    "textToAudio",
                    piperVoiceClass,
                    String::class.java
                )
                val audioData = textToAudioMethod.invoke(piper, voiceModel, text) as ShortArray
                
                Result.success(PiperAudioStream(audioData))
            } catch (e: Exception) {
                pluginContext?.log(LogLevel.ERROR, "TTS synthesis failed: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    override fun getAvailableVoices(): List<VoiceModel> {
        return PIPER_VOICES
    }
    
    override fun supportsStreaming(): Boolean = false
    
    override fun getAudioFormat(): AudioFormat {
        return AudioFormat(
            encoding = AudioEncoding.PCM,
            sampleRate = 22050,
            channels = 1,
            bitDepth = 16
        )
    }
    
    /**
     * Load a voice model by ID using reflection.
     */
    private fun loadVoice(voiceId: String): Any? {
        val piper = piperInstance ?: return null
        val voicesDirectory = voicesDir ?: return null
        
        // Check if voice is already loaded
        if (currentVoice != null && currentVoiceId == voiceId) {
            return currentVoice
        }
        
        // Close previous voice
        if (currentVoice != null && piperVoiceClass != null) {
            try {
                val closeMethod = piperVoiceClass!!.getMethod("close")
                closeMethod.invoke(currentVoice)
            } catch (e: Exception) {
                // Ignore
            }
        }
        currentVoice = null
        currentVoiceId = null
        
        // Find voice info
        val voiceInfo = PIPER_VOICES.find { it.id == voiceId } ?: return null
        
        // Check if voice files exist
        val modelFile = File(voicesDirectory, "${voiceId}.onnx")
        val configFile = File(voicesDirectory, "${voiceId}.onnx.json")
        
        if (!modelFile.exists() || !configFile.exists()) {
            pluginContext?.log(LogLevel.WARN, "Voice files not found for $voiceId. Please download the voice first.")
            return null
        }
        
        return try {
            // Load voice using reflection
            val loadVoiceMethod = piperJNIClass!!.getMethod(
                "loadVoice",
                Path::class.java,
                Path::class.java
            )
            val voice = loadVoiceMethod.invoke(piper, modelFile.toPath(), configFile.toPath())
            currentVoice = voice
            currentVoiceId = voiceId
            voice
        } catch (e: Exception) {
            pluginContext?.log(LogLevel.ERROR, "Failed to load voice $voiceId: ${e.message}")
            null
        }
    }
    
    private var currentVoiceId: String? = null
    
    companion object {
        /**
         * Available Piper voices.
         */
        val PIPER_VOICES = listOf(
            // English (US)
            VoiceModel(
                id = "en_US-amy-medium",
                name = "Amy (English US)",
                language = "en-US",
                gender = VoiceGender.FEMALE,
                requiresDownload = true,
                downloadSize = 63_000_000
            ),
            VoiceModel(
                id = "en_US-lessac-medium",
                name = "Lessac (English US)",
                language = "en-US",
                gender = VoiceGender.FEMALE,
                requiresDownload = true,
                downloadSize = 63_000_000
            ),
            VoiceModel(
                id = "en_US-ryan-medium",
                name = "Ryan (English US)",
                language = "en-US",
                gender = VoiceGender.MALE,
                requiresDownload = true,
                downloadSize = 63_000_000
            ),
            // English (UK)
            VoiceModel(
                id = "en_GB-alan-medium",
                name = "Alan (English UK)",
                language = "en-GB",
                gender = VoiceGender.MALE,
                requiresDownload = true,
                downloadSize = 63_000_000
            ),
            // German
            VoiceModel(
                id = "de_DE-thorsten-medium",
                name = "Thorsten (German)",
                language = "de-DE",
                gender = VoiceGender.MALE,
                requiresDownload = true,
                downloadSize = 63_000_000
            ),
            // French
            VoiceModel(
                id = "fr_FR-siwis-medium",
                name = "Siwis (French)",
                language = "fr-FR",
                gender = VoiceGender.FEMALE,
                requiresDownload = true,
                downloadSize = 63_000_000
            ),
            // Spanish
            VoiceModel(
                id = "es_ES-sharvard-medium",
                name = "Sharvard (Spanish)",
                language = "es-ES",
                gender = VoiceGender.MALE,
                requiresDownload = true,
                downloadSize = 63_000_000
            ),
            // Russian
            VoiceModel(
                id = "ru_RU-irina-medium",
                name = "Irina (Russian)",
                language = "ru-RU",
                gender = VoiceGender.FEMALE,
                requiresDownload = true,
                downloadSize = 63_000_000
            ),
            // Chinese
            VoiceModel(
                id = "zh_CN-huayan-medium",
                name = "Huayan (Chinese)",
                language = "zh-CN",
                gender = VoiceGender.FEMALE,
                requiresDownload = true,
                downloadSize = 63_000_000
            ),
            // Japanese
            VoiceModel(
                id = "ja_JP-kokoro-medium",
                name = "Kokoro (Japanese)",
                language = "ja-JP",
                gender = VoiceGender.FEMALE,
                requiresDownload = true,
                downloadSize = 63_000_000
            )
        )
    }
}

/**
 * Audio stream implementation for Piper TTS output.
 */
class PiperAudioStream(private val audioSamples: ShortArray) : AudioStream {
    
    private var position = 0
    
    override suspend fun read(buffer: ByteArray): Int {
        if (position >= audioSamples.size) return -1
        
        val samplesToRead = minOf(buffer.size / 2, audioSamples.size - position)
        var bufferPos = 0
        
        for (i in 0 until samplesToRead) {
            val sample = audioSamples[position + i]
            buffer[bufferPos++] = (sample.toInt() and 0xFF).toByte()
            buffer[bufferPos++] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        
        position += samplesToRead
        return samplesToRead * 2
    }
    
    override fun close() {
        position = audioSamples.size
    }
    
    override fun getDuration(): Long? {
        // Duration = samples / sampleRate * 1000ms
        return (audioSamples.size.toLong() * 1000) / 22050
    }
}
