package io.github.ireaderorg.plugins.graalvmengine

import ireader.plugin.api.*
import java.lang.reflect.Method

/**
 * GraalVM JavaScript Engine Plugin for Desktop.
 * 
 * Provides GraalVM Polyglot JavaScript engine capabilities for running
 * LNReader-compatible source plugins on Desktop (Windows, macOS, Linux).
 * 
 * This plugin uses reflection to access GraalVM classes at runtime,
 * allowing it to compile without platform-specific dependencies.
 * The actual GraalVM library must be provided by the host app.
 * 
 * Features:
 * - Full ES2022+ support
 * - High-performance JIT compilation
 * - Native Promise and async/await support
 * - ES Modules support
 * - ~40-60MB per platform
 */
class GraalVMEnginePlugin : JSEnginePlugin {
    
    private var pluginContext: PluginContext? = null
    private var isAvailable = false
    private var contextBuilderClass: Class<*>? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.graalvm-engine",
        name = "GraalVM JavaScript Engine",
        version = "25.0.1",
        versionCode = 1,
        description = "GraalVM Polyglot JavaScript engine for Desktop - high-performance JS execution with full ES2022+ support",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.JS_ENGINE,
        permissions = listOf(PluginPermission.STORAGE),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.DESKTOP), // Desktop only!
        nativeLibraries = mapOf(
            "windows-x64" to listOf(
                "native/windows-x64/polyglot.dll",
                "native/windows-x64/js.dll"
            ),
            "macos-x64" to listOf(
                "native/macos-x64/libpolyglot.dylib",
                "native/macos-x64/libjs.dylib"
            ),
            "macos-arm64" to listOf(
                "native/macos-arm64/libpolyglot.dylib",
                "native/macos-arm64/libjs.dylib"
            ),
            "linux-x64" to listOf(
                "native/linux-x64/libpolyglot.so",
                "native/linux-x64/libjs.so"
            )
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        
        // Check platform - this plugin is Desktop-only
        if (context.getPlatform() != Platform.DESKTOP) {
            context.log(LogLevel.WARN, "GraalVM engine is only available on Desktop")
            isAvailable = false
            return
        }
        
        // Try to load GraalVM classes via reflection
        isAvailable = try {
            val contextClass = Class.forName("org.graalvm.polyglot.Context")
            contextBuilderClass = Class.forName("org.graalvm.polyglot.Context\$Builder")
            
            // Test by creating a context
            val newBuilderMethod = contextClass.getMethod("newBuilder", Array<String>::class.java)
            val builder = newBuilderMethod.invoke(null, arrayOf("js"))
            val buildMethod = contextBuilderClass!!.getMethod("build")
            val testContext = buildMethod.invoke(builder)
            val closeMethod = contextClass.getMethod("close")
            closeMethod.invoke(testContext)
            
            context.log(LogLevel.INFO, "GraalVM engine is available")
            true
        } catch (e: ClassNotFoundException) {
            context.log(LogLevel.ERROR, "GraalVM library not found - please ensure it's bundled with the app")
            false
        } catch (e: Exception) {
            context.log(LogLevel.ERROR, "GraalVM engine not available: ${e.message}")
            false
        }
    }
    
    override fun cleanup() {
        pluginContext = null
        isAvailable = false
        contextBuilderClass = null
    }
    
    override fun createEngine(): JSEngineInstance {
        if (!isAvailable) {
            throw IllegalStateException("GraalVM engine not available")
        }
        return GraalVMEngineInstance()
    }
    
    override fun getCapabilities(): JSEngineCapabilities {
        return JSEngineCapabilities(
            engineName = "GraalVM Polyglot",
            engineVersion = "25.0.1",
            ecmaScriptVersion = "ES2022",
            supportsModules = true,
            supportsAsync = true,
            supportsPromises = true,
            supportsWasm = true,
            maxMemoryBytes = 0,
            supportsDebugging = true
        )
    }
    
    override fun isAvailable(): Boolean = isAvailable
}

/**
 * GraalVM engine instance using reflection to access GraalVM classes.
 */
class GraalVMEngineInstance : JSEngineInstance {
    
    private var context: Any? = null
    private var isDisposed = false
    
    // Cached classes
    private val contextClass by lazy { Class.forName("org.graalvm.polyglot.Context") }
    private val valueClass by lazy { Class.forName("org.graalvm.polyglot.Value") }
    private val sourceClass by lazy { Class.forName("org.graalvm.polyglot.Source") }
    
    override suspend fun initialize() {
        if (isDisposed) {
            throw IllegalStateException("Engine instance has been disposed")
        }
        if (context != null) return
        
        // Create context using reflection
        val builderClass = Class.forName("org.graalvm.polyglot.Context\$Builder")
        val newBuilderMethod = contextClass.getMethod("newBuilder", Array<String>::class.java)
        val builder = newBuilderMethod.invoke(null, arrayOf("js"))
        
        // Configure builder
        val optionMethod = builderClass.getMethod("option", String::class.java, String::class.java)
        optionMethod.invoke(builder, "js.ecmascript-version", "2022")
        optionMethod.invoke(builder, "engine.WarnInterpreterOnly", "false")
        
        val buildMethod = builderClass.getMethod("build")
        context = buildMethod.invoke(builder)
        
        // Setup CommonJS-like module system
        val evalMethod = contextClass.getMethod("eval", String::class.java, CharSequence::class.java)
        evalMethod.invoke(context, "js", """
            globalThis.eval = undefined;
            if (typeof module === 'undefined') {
                globalThis.module = { exports: {} };
            }
            if (typeof exports === 'undefined') {
                globalThis.exports = globalThis.module.exports;
            }
            globalThis.importScripts = undefined;
        """.trimIndent())
    }
    
    override suspend fun evaluate(code: String): JSValue {
        return evaluate(code, "<eval>")
    }
    
    override suspend fun evaluate(code: String, scriptName: String): JSValue {
        checkState()
        val evalMethod = contextClass.getMethod("eval", String::class.java, CharSequence::class.java)
        val result = evalMethod.invoke(context, "js", code)
        return ReflectiveGraalValue(valueClass, result)
    }
    
    override suspend fun callFunction(functionName: String, vararg args: Any?): JSValue {
        checkState()
        
        // Get bindings
        val getBindingsMethod = contextClass.getMethod("getBindings", String::class.java)
        val bindings = getBindingsMethod.invoke(context, "js")
        
        // Get function
        val getMemberMethod = valueClass.getMethod("getMember", String::class.java)
        val function = getMemberMethod.invoke(bindings, functionName)
        
        // Check if executable
        val canExecuteMethod = valueClass.getMethod("canExecute")
        if (canExecuteMethod.invoke(function) != true) {
            throw IllegalArgumentException("$functionName is not a function")
        }
        
        // Execute
        val executeMethod = valueClass.getMethod("execute", Array<Any>::class.java)
        val result = executeMethod.invoke(function, args)
        return ReflectiveGraalValue(valueClass, result)
    }
    
    override fun getGlobal(name: String): JSValue? {
        checkState()
        val getBindingsMethod = contextClass.getMethod("getBindings", String::class.java)
        val bindings = getBindingsMethod.invoke(context, "js")
        val getMemberMethod = valueClass.getMethod("getMember", String::class.java)
        val value = getMemberMethod.invoke(bindings, name)
        
        val isNullMethod = valueClass.getMethod("isNull")
        return if (value == null || isNullMethod.invoke(value) == true) null 
               else ReflectiveGraalValue(valueClass, value)
    }
    
    override fun setGlobal(name: String, value: Any?) {
        checkState()
        val getBindingsMethod = contextClass.getMethod("getBindings", String::class.java)
        val bindings = getBindingsMethod.invoke(context, "js")
        val putMemberMethod = valueClass.getMethod("putMember", String::class.java, Any::class.java)
        putMemberMethod.invoke(bindings, name, value)
    }
    
    override fun registerFunction(name: String, function: JSNativeFunction) {
        checkState()
        // Note: Registering functions via reflection with ProxyExecutable is complex
        // For now, this is a simplified implementation
    }
    
    override fun dispose() {
        if (!isDisposed && context != null) {
            try {
                val closeMethod = contextClass.getMethod("close")
                closeMethod.invoke(context)
            } catch (e: Exception) {
                // Ignore disposal errors
            }
            context = null
            isDisposed = true
        }
    }
    
    override fun isValid(): Boolean = context != null && !isDisposed
    
    private fun checkState() {
        if (context == null) throw IllegalStateException("Engine not initialized")
        if (isDisposed) throw IllegalStateException("Engine has been disposed")
    }
}

/**
 * JSValue implementation using reflection to access GraalVM Value.
 */
private class ReflectiveGraalValue(
    private val valueClass: Class<*>,
    private val value: Any?
) : JSValue {
    
    private fun invokeMethod(name: String, vararg args: Any?): Any? {
        if (value == null) return null
        return try {
            val method = if (args.isEmpty()) {
                valueClass.getMethod(name)
            } else {
                valueClass.getMethod(name, *args.map { it?.javaClass ?: Any::class.java }.toTypedArray())
            }
            method.invoke(value, *args)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun isNullOrUndefined(): Boolean {
        if (value == null) return true
        return invokeMethod("isNull") == true
    }
    
    override fun isString() = invokeMethod("isString") == true
    override fun isNumber() = invokeMethod("isNumber") == true
    override fun isBoolean() = invokeMethod("isBoolean") == true
    override fun isObject() = invokeMethod("hasMembers") == true && invokeMethod("hasArrayElements") != true
    override fun isArray() = invokeMethod("hasArrayElements") == true
    override fun isFunction() = invokeMethod("canExecute") == true
    
    override fun asString(): String? {
        return if (isString()) invokeMethod("asString") as? String else null
    }
    
    override fun asInt(): Int? {
        return if (invokeMethod("fitsInInt") == true) invokeMethod("asInt") as? Int else null
    }
    
    override fun asLong(): Long? {
        return if (invokeMethod("fitsInLong") == true) invokeMethod("asLong") as? Long else null
    }
    
    override fun asDouble(): Double? {
        return if (invokeMethod("fitsInDouble") == true) invokeMethod("asDouble") as? Double else null
    }
    
    override fun asBoolean(): Boolean? {
        return if (isBoolean()) invokeMethod("asBoolean") as? Boolean else null
    }
    
    override fun getProperty(key: String): JSValue? {
        if (invokeMethod("hasMembers") != true) return null
        val getMemberMethod = valueClass.getMethod("getMember", String::class.java)
        val prop = getMemberMethod.invoke(value, key)
        val isNullMethod = valueClass.getMethod("isNull")
        return if (prop == null || isNullMethod.invoke(prop) == true) null 
               else ReflectiveGraalValue(valueClass, prop)
    }
    
    override fun setProperty(key: String, propValue: Any?) {
        if (value == null) return
        try {
            val putMemberMethod = valueClass.getMethod("putMember", String::class.java, Any::class.java)
            putMemberMethod.invoke(value, key, propValue)
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    override fun getKeys(): List<String> {
        if (value == null) return emptyList()
        return try {
            val memberKeysMethod = valueClass.getMethod("getMemberKeys")
            @Suppress("UNCHECKED_CAST")
            (memberKeysMethod.invoke(value) as? Set<String>)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun getArrayElement(index: Int): JSValue? {
        if (invokeMethod("hasArrayElements") != true) return null
        return try {
            val arraySizeMethod = valueClass.getMethod("getArraySize")
            val size = (arraySizeMethod.invoke(value) as Long).toInt()
            if (index < 0 || index >= size) return null
            
            val getArrayElementMethod = valueClass.getMethod("getArrayElement", Long::class.java)
            val element = getArrayElementMethod.invoke(value, index.toLong())
            ReflectiveGraalValue(valueClass, element)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getArrayLength(): Int {
        return try {
            val arraySizeMethod = valueClass.getMethod("getArraySize")
            (arraySizeMethod.invoke(value) as? Long)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    override fun toJson(): String {
        return when {
            value == null || isNullOrUndefined() -> "null"
            isString() -> "\"${asString()}\""
            isNumber() || isBoolean() -> value.toString()
            isArray() -> {
                val items = (0 until getArrayLength()).map { i ->
                    getArrayElement(i)?.toJson() ?: "null"
                }
                "[${items.joinToString(",")}]"
            }
            isObject() -> {
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
