package io.github.ireaderorg.plugins.j2v8engine

import ireader.plugin.api.*
import java.lang.reflect.Method

/**
 * J2V8 JavaScript Engine Plugin for Android.
 * 
 * Provides V8 JavaScript engine capabilities for running LNReader-compatible
 * source plugins on Android devices.
 * 
 * This plugin uses reflection to access J2V8 classes at runtime,
 * allowing it to compile without Android-specific dependencies.
 * The actual J2V8 library must be provided by the host app.
 * 
 * Features:
 * - Full ES6+ support (same engine as Chrome/Node.js)
 * - Native Promise and async/await support
 * - High performance JIT compilation
 * - ~15-20MB per ABI (arm64, arm32, x64, x86)
 */
class J2V8EnginePlugin : JSEnginePlugin {
    
    private var pluginContext: PluginContext? = null
    private var isAvailable = false
    private var v8Class: Class<*>? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.j2v8-engine",
        name = "J2V8 JavaScript Engine",
        version = "6.3.4",
        versionCode = 1,
        description = "V8 JavaScript engine for Android - enables LNReader-compatible sources with full ES6+ support",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.JS_ENGINE,
        permissions = listOf(PluginPermission.STORAGE),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID), // Android only!
        nativeLibraries = mapOf(
            "android-arm64" to listOf("native/android-arm64/libj2v8.so"),
            "android-arm32" to listOf("native/android-arm32/libj2v8.so"),
            "android-x64" to listOf("native/android-x64/libj2v8.so"),
            "android-x86" to listOf("native/android-x86/libj2v8.so")
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        
        // Check platform - this plugin is Android-only
        if (context.getPlatform() != Platform.ANDROID) {
            context.log(LogLevel.WARN, "J2V8 engine is only available on Android")
            isAvailable = false
            return
        }
        
        // Try to load J2V8 classes via reflection
        isAvailable = try {
            v8Class = Class.forName("com.eclipsesource.v8.V8")
            val createRuntime = v8Class!!.getMethod("createV8Runtime")
            val testV8 = createRuntime.invoke(null)
            val releaseMethod = v8Class!!.getMethod("release")
            releaseMethod.invoke(testV8)
            context.log(LogLevel.INFO, "J2V8 engine is available")
            true
        } catch (e: ClassNotFoundException) {
            context.log(LogLevel.ERROR, "J2V8 library not found - please ensure it's bundled with the app")
            false
        } catch (e: Exception) {
            context.log(LogLevel.ERROR, "J2V8 engine not available: ${e.message}")
            false
        }
    }
    
    override fun cleanup() {
        pluginContext = null
        isAvailable = false
        v8Class = null
    }
    
    override fun createEngine(): JSEngineInstance {
        if (!isAvailable || v8Class == null) {
            throw IllegalStateException("J2V8 engine not available")
        }
        return J2V8EngineInstance(v8Class!!)
    }
    
    override fun getCapabilities(): JSEngineCapabilities {
        return JSEngineCapabilities(
            engineName = "J2V8 (V8)",
            engineVersion = "6.3.4",
            ecmaScriptVersion = "ES2022",
            supportsModules = false,
            supportsAsync = true,
            supportsPromises = true,
            supportsWasm = false,
            maxMemoryBytes = 0,
            supportsDebugging = false
        )
    }
    
    override fun isAvailable(): Boolean = isAvailable
}

/**
 * J2V8 engine instance using reflection to access V8 classes.
 */
class J2V8EngineInstance(private val v8Class: Class<*>) : JSEngineInstance {
    
    private var v8Instance: Any? = null
    private var isDisposed = false
    
    override suspend fun initialize() {
        if (isDisposed) {
            throw IllegalStateException("Engine instance has been disposed")
        }
        if (v8Instance != null) return
        
        val createRuntime = v8Class.getMethod("createV8Runtime")
        v8Instance = createRuntime.invoke(null)
        
        // Setup CommonJS-like module system
        val executeScriptMethod = v8Class.getMethod("executeScript", String::class.java)
        executeScriptMethod.invoke(v8Instance, """
            if (typeof module === 'undefined') {
                var module = { exports: {} };
            }
            if (typeof exports === 'undefined') {
                var exports = module.exports;
            }
        """.trimIndent())
    }
    
    override suspend fun evaluate(code: String): JSValue {
        return evaluate(code, "<eval>")
    }
    
    override suspend fun evaluate(code: String, scriptName: String): JSValue {
        checkState()
        val executeScriptMethod = v8Class.getMethod("executeScript", String::class.java)
        val result = executeScriptMethod.invoke(v8Instance, code)
        return ReflectiveJ2V8Value(v8Class, result)
    }
    
    override suspend fun callFunction(functionName: String, vararg args: Any?): JSValue {
        checkState()
        
        // Get the function object via reflection
        val getObjectMethod = v8Class.getMethod("getObject", String::class.java)
        val func = getObjectMethod.invoke(v8Instance, functionName)
            ?: throw IllegalArgumentException("$functionName not found")
        
        val v8FunctionClass = Class.forName("com.eclipsesource.v8.V8Function")
        if (!v8FunctionClass.isInstance(func)) {
            throw IllegalArgumentException("$functionName is not a function")
        }
        
        // Create V8Array for arguments
        val v8ArrayClass = Class.forName("com.eclipsesource.v8.V8Array")
        val v8ArrayConstructor = v8ArrayClass.getConstructor(v8Class)
        val v8Args = v8ArrayConstructor.newInstance(v8Instance)
        
        try {
            // Push arguments using reflection
            args.forEach { arg ->
                when (arg) {
                    null -> v8ArrayClass.getMethod("pushNull").invoke(v8Args)
                    is String -> v8ArrayClass.getMethod("push", String::class.java).invoke(v8Args, arg)
                    is Int -> v8ArrayClass.getMethod("push", Int::class.javaPrimitiveType).invoke(v8Args, arg)
                    is Double -> v8ArrayClass.getMethod("push", Double::class.javaPrimitiveType).invoke(v8Args, arg)
                    is Boolean -> v8ArrayClass.getMethod("push", Boolean::class.javaPrimitiveType).invoke(v8Args, arg)
                    else -> v8ArrayClass.getMethod("push", String::class.java).invoke(v8Args, arg.toString())
                }
            }
            
            // Call function
            val callMethod = v8FunctionClass.getMethod("call", v8Class, v8ArrayClass)
            val result = callMethod.invoke(func, v8Instance, v8Args)
            return ReflectiveJ2V8Value(v8Class, result)
        } finally {
            v8ArrayClass.getMethod("release").invoke(v8Args)
            v8FunctionClass.getMethod("release").invoke(func)
        }
    }
    
    override fun getGlobal(name: String): JSValue? {
        checkState()
        val getMethod = v8Class.getMethod("get", String::class.java)
        val result = getMethod.invoke(v8Instance, name)
        val undefined = v8Class.getMethod("getUndefined").invoke(null)
        return if (result == undefined) null else ReflectiveJ2V8Value(v8Class, result)
    }
    
    override fun setGlobal(name: String, value: Any?) {
        checkState()
        when (value) {
            null -> v8Class.getMethod("addNull", String::class.java).invoke(v8Instance, name)
            is String -> v8Class.getMethod("add", String::class.java, String::class.java).invoke(v8Instance, name, value)
            is Int -> v8Class.getMethod("add", String::class.java, Int::class.javaPrimitiveType).invoke(v8Instance, name, value)
            is Double -> v8Class.getMethod("add", String::class.java, Double::class.javaPrimitiveType).invoke(v8Instance, name, value)
            is Boolean -> v8Class.getMethod("add", String::class.java, Boolean::class.javaPrimitiveType).invoke(v8Instance, name, value)
            else -> v8Class.getMethod("add", String::class.java, String::class.java).invoke(v8Instance, name, value.toString())
        }
    }
    
    override fun registerFunction(name: String, function: JSNativeFunction) {
        // Note: Registering Java methods via reflection is complex
        // This would require dynamic proxy generation
        checkState()
    }
    
    override fun dispose() {
        if (!isDisposed && v8Instance != null) {
            try {
                v8Class.getMethod("release").invoke(v8Instance)
            } catch (e: Exception) {
                // Ignore disposal errors
            }
            v8Instance = null
            isDisposed = true
        }
    }
    
    override fun isValid(): Boolean = v8Instance != null && !isDisposed
    
    private fun checkState() {
        if (v8Instance == null) throw IllegalStateException("Engine not initialized")
        if (isDisposed) throw IllegalStateException("Engine has been disposed")
    }
}

/**
 * JSValue implementation using reflection to access V8 values.
 */
private class ReflectiveJ2V8Value(
    private val v8Class: Class<*>,
    private val value: Any?
) : JSValue {
    
    private val v8ObjectClass by lazy { Class.forName("com.eclipsesource.v8.V8Object") }
    private val v8ArrayClass by lazy { Class.forName("com.eclipsesource.v8.V8Array") }
    private val v8FunctionClass by lazy { Class.forName("com.eclipsesource.v8.V8Function") }
    private val undefined by lazy { v8Class.getMethod("getUndefined").invoke(null) }
    
    override fun isNullOrUndefined() = value == null || value == undefined
    override fun isString() = value is String
    override fun isNumber() = value is Number
    override fun isBoolean() = value is Boolean
    override fun isObject() = v8ObjectClass.isInstance(value) && !v8ArrayClass.isInstance(value)
    override fun isArray() = v8ArrayClass.isInstance(value)
    override fun isFunction() = v8FunctionClass.isInstance(value)
    
    override fun asString(): String? = value as? String
    override fun asInt(): Int? = (value as? Number)?.toInt()
    override fun asLong(): Long? = (value as? Number)?.toLong()
    override fun asDouble(): Double? = (value as? Number)?.toDouble()
    override fun asBoolean(): Boolean? = value as? Boolean
    
    override fun getProperty(key: String): JSValue? {
        if (!v8ObjectClass.isInstance(value)) return null
        val getMethod = v8ObjectClass.getMethod("get", String::class.java)
        val prop = getMethod.invoke(value, key)
        return if (prop == undefined) null else ReflectiveJ2V8Value(v8Class, prop)
    }
    
    override fun setProperty(key: String, propValue: Any?) {
        if (!v8ObjectClass.isInstance(value)) return
        when (propValue) {
            null -> v8ObjectClass.getMethod("addNull", String::class.java).invoke(value, key)
            is String -> v8ObjectClass.getMethod("add", String::class.java, String::class.java).invoke(value, key, propValue)
            is Int -> v8ObjectClass.getMethod("add", String::class.java, Int::class.javaPrimitiveType).invoke(value, key, propValue)
            is Double -> v8ObjectClass.getMethod("add", String::class.java, Double::class.javaPrimitiveType).invoke(value, key, propValue)
            is Boolean -> v8ObjectClass.getMethod("add", String::class.java, Boolean::class.javaPrimitiveType).invoke(value, key, propValue)
            else -> v8ObjectClass.getMethod("add", String::class.java, String::class.java).invoke(value, key, propValue.toString())
        }
    }
    
    override fun getKeys(): List<String> {
        if (!v8ObjectClass.isInstance(value)) return emptyList()
        val getKeysMethod = v8ObjectClass.getMethod("getKeys")
        @Suppress("UNCHECKED_CAST")
        return (getKeysMethod.invoke(value) as? Array<String>)?.toList() ?: emptyList()
    }
    
    override fun getArrayElement(index: Int): JSValue? {
        if (!v8ArrayClass.isInstance(value)) return null
        val lengthMethod = v8ArrayClass.getMethod("length")
        val length = lengthMethod.invoke(value) as Int
        if (index < 0 || index >= length) return null
        val getMethod = v8ArrayClass.getMethod("get", Int::class.javaPrimitiveType)
        return ReflectiveJ2V8Value(v8Class, getMethod.invoke(value, index))
    }
    
    override fun getArrayLength(): Int {
        if (!v8ArrayClass.isInstance(value)) return 0
        val lengthMethod = v8ArrayClass.getMethod("length")
        return lengthMethod.invoke(value) as Int
    }
    
    override fun toJson(): String {
        return when {
            value == null || value == undefined -> "null"
            value is String -> "\"$value\""
            value is Number || value is Boolean -> value.toString()
            v8ArrayClass.isInstance(value) -> {
                val length = getArrayLength()
                val items = (0 until length).map { i ->
                    getArrayElement(i)?.toJson() ?: "null"
                }
                "[${items.joinToString(",")}]"
            }
            v8ObjectClass.isInstance(value) -> {
                val pairs = getKeys().map { key ->
                    "\"$key\":${getProperty(key)?.toJson() ?: "null"}"
                }
                "{${pairs.joinToString(",")}}"
            }
            else -> "\"$value\""
        }
    }
    
    override fun getNativeValue(): Any? = value
}
